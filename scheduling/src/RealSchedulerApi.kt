import java.text.SimpleDateFormat
import java.util.Calendar

class RealSchedulerApi() : SchedulerApi {
  override val scheduler = Scheduler()

  /**
   * Creates and saves an event for a given employeeId
   * Also builds shifts for an event with an employee from the start to end date for the given times
   * If no end date is provided, shifts are created until the end of the current year.
   **/
  override fun scheduleEventForEmployee(
    employeeId: Int,
    startDate: String,
    endDate: String?,
    startTime: String,
    duration: Float,
  ): Boolean {
    //if duration > 24H, throw
    check(duration <= 24F) {
      "Duration of an event must be less than or equal to 24 hours."
    }

    val event = scheduler.buildEvent(
      eventId = scheduler.eventIdForClass.toInt(),
      startTime = startTime,
      duration = duration,
    )
    scheduler.events.add(event)

    //increment the event id everytime this method is invoked
    scheduler.eventIdForClass.incrementAndGet()

    val shifts = scheduler.buildShifts(
      startTime = event.startTime,
      endTime = event.endTime,
      startDate = startDate,
      endDate = endDate,
      duration = duration,
      employeeId = employeeId,
      eventId = event.id
    )
    scheduler.employeeSchedules.put(event.id, shifts)
    return true
  }

  /**
   * Can modify the details of an event with a new start and end time
   * Overrides or adds shifts for an event
   * If no new start time or duration are provided, the event's existing details are used
   * If no end date is provided, shifts are overrided till the end of the year
   * This function does not handle the case of an employee being double booked for mutliple shifts.
   */
  override fun scheduleOverrideForEvent(
    eventId: Int,
    overrideType: String,
    overrideEmployeeId: Int,
    overrideStartDate: String,
    newEndDate: String?,
    newStartTime: String?,
    newDuration: Float?,
  ): Boolean {

    val event = scheduler.events.filter { event -> event.id == eventId }.singleOrNull()
    checkNotNull(event) {
      "This event does not exist in the system."
    }

    val duration = newDuration ?: event.duration
    val startTime = newStartTime ?: event.startTime

    val modifiedEvent = scheduler.buildEvent(
      eventId = eventId,
      startTime = startTime,
      duration = duration
    )

    scheduler.modifyEventList(modifiedEvent)
    val oldShifts = scheduler.employeeSchedules.get(modifiedEvent.id)!!

    if (overrideType == TODAY_FORWARD) {
      val newShifts = scheduler.buildShifts(
        startTime = modifiedEvent.startTime,
        endTime = modifiedEvent.endTime,
        startDate = overrideStartDate,
        endDate = newEndDate,
        duration = modifiedEvent.duration,
        employeeId = overrideEmployeeId,
        eventId = event.id
      )

      val (minDate, maxDate) = scheduler.getMinAndMaxDatesforEvent(oldShifts)
      //build a new list of shifts
      val updatedShifts = scheduler.modifyShiftList(
        oldShifts = oldShifts,
        newShifts = newShifts,
        minDate = minDate,
        maxDate = maxDate
      )
      //save the updated shift list
      scheduler.employeeSchedules.put(eventId, updatedShifts)
    } else if (overrideType == TODAY_ONLY) {

      //set the end date to be the same as the start date for "TODAY_ONLY" events
      val newShifts = scheduler.buildShifts(
        startTime = modifiedEvent.startTime,
        endTime = modifiedEvent.endTime,
        startDate = overrideStartDate,
        endDate = overrideStartDate,
        duration = modifiedEvent.duration,
        employeeId = overrideEmployeeId,
        eventId = event.id
      )
      val newShift = newShifts.singleOrNull()!!

      //replace old shift with the new shift
      for (i in oldShifts.indices) {
        if (oldShifts[i].date == newShift.date) {
          oldShifts[i] = newShift
        }
      }
      //save the updated shifts
      scheduler.employeeSchedules.put(eventId, oldShifts)
    }

    return true
  }

  /**
   * Given a start date and a number of days,
   * prints the schedule for each day in the following format
   *
   * Printing the schedule for 2023-03-05 to 2023-03-11
   * ========== 2023-03-05 ==========
   * Event 1 | Date: 2023-03-05 | Shift Time: 08:00:00 -07:00 - 16:00:00 -07:00 | 8 hrs | Employee 1
   * Event 2 | Date: 2023-03-05 | Shift Time: 06:00:00 -07:00 - 13:00:00 -07:00 | 7 hrs | Employee 8
   *
   * If events are not available on a certain day, the following will be printed:
   * Printing the schedule for 2023-02-04 to 2023-02-08
   * No events in schedule.
   **/

  override fun printEventsForRange(
    startDate: String,
    numDays: Int,
  ) {

    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy-MM-dd")
    calendar.setTime(sdf.parse(startDate))

    val (year, month, date) = startDate.split("-")

    scheduler.printEvents(
      startDate = startDate,
      dateFormatter = sdf,
      calendar = calendar,
      numDays = numDays,
    )
  }

/**
 * Prints the entire schedule of events with corresponding shifts in the
 * following format:
 * Printing the schedule for Event 1
 * Event 1 | Date: 2023-03-01 | Shift Time: 08:00:00 -07:00 - 16:00:00 -07:00 | 8 hrs | Employee 1
 * Event 1 | Date: 2023-03-02 | Shift Time: 08:00:00 -07:00 - 16:00:00 -07:00 | 8 hrs | Employee 1
 * Event 1 | Date: 2023-03-03 | Shift Time: 08:00:00 -07:00 - 16:00:00 -07:00 | 8 hrs | Employee 1
 *
 **/
override fun printFullSchedule(){
  var schedule = StringBuilder()

  for (event in scheduler.events) {
    schedule.append("Printing the schedule for Event ${event.id}\n")
    for (shift in scheduler.employeeSchedules.get(event.id)!!) {

      val shiftDurationFormatted = if (shift.duration.rem(shift.duration.toInt()) > 0) shift.duration.toString()
        else shift.duration.toInt().toString()

      scheduler.appendDetailsToString(
        schedule = schedule,
        event = event,
        shift = shift,
        shiftDuration = shiftDurationFormatted
      )
    }
    schedule.append("\n")
  }
  println(schedule.toString())
}

  companion object {
    val TODAY_ONLY = "TODAY_ONLY"
    val TODAY_FORWARD = "TODAY_FORWARD"
  }
}
