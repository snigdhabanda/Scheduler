import java.text.SimpleDateFormat
import java.util.Calendar

class RealSchedulerApi() : SchedulerApi {
  override val scheduler = Scheduler()

  /**
   * 1) Creates and saves an event with a start and end time for an employee
   * 2) Builds shifts for an event with a date range and assigns the shift to an employee
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
    //save the event
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

    //save the shifts for an event
    scheduler.employeeSchedules.put(event.id, shifts)
    return true
  }

  /**
   * Modifies the details of an event to have a new start and/or end time
   * Overrides existing shifts or adds new ones with the event's new details
   * If no new start time or duration are provided, the event's previous details are used
   * If no end date is provided, shifts are overrided till the end of the year
   *
   * An override type of TODAY_FORWARD will override shifts for an event for the new date range
   * An override type of TODAY_ONLY will override shifts for an event only on the new start date
   *
   * This API does not handle the case of an employee being double booked for mutliple shifts
   * It also does not allow employees to be assigned to multiple shifts in one day
   * Only one employee can be assigned at max to each day.
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

    //set the duration and start time if not provided
    val duration = newDuration ?: event.duration
    val startTime = newStartTime ?: event.startTime

    val modifiedEvent = scheduler.buildEvent(
      eventId = eventId,
      startTime = startTime,
      duration = duration
    )

    //updates details of an event
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

      //get the date range for the set of existing shifts
      //this guides how new shifts should be added to the existing list
      //also see Scheduler.modifyEventList()
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
   * print the schedule for each day in the following format
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
