import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicLong

class Scheduler {
  val employeeSchedules: MutableMap<Int, MutableList<Shift>> = mutableMapOf()
  val events: MutableList<Event> = mutableListOf()

  val eventIdForClass: AtomicLong = AtomicLong(1)

  /**
   * Creates an event given necessary details.
   */
  fun buildEvent(
    eventId: Int? = this.eventIdForClass.toInt(),
    startTime: String,
    duration: Float,
  ): Event {
    val endTime = getEndTime(startTime, duration)

    return Event(
      id = eventId!!,
      startTime = formatTime(startTime),
      endTime = formatTime(endTime),
      duration = duration
    )
  }

  /**
   * Creates a shift given necessary details.
   */
  fun buildShift(
    startTime: String,
    endTime: String,
    date: String,
    duration: Float,
    employeeId: Int,
    eventId: Int,
  ): Shift {

    val shift = Shift(
      startTime = startTime,
      endTime = endTime,
      employeeId = employeeId,
      eventId = eventId,
      duration = duration,
      date = date
    )

    return shift
  }

  /**
   * Return a list of shifts for an event mapped to an employee.
   * If the end date for a shift range is not provided, shifts are created
   * till the end of the start date's year.
   */
  fun buildShifts(
    startDate: String,
    endDate: String?,
    employeeId: Int,
    startTime: String,
    endTime: String,
    duration: Float,
    eventId: Int,
  ): MutableList<Shift> {
    val shifts = mutableListOf<Shift>()

    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var startDateFormatted = LocalDate.parse(startDate, dateTimeFormatter)
    var endDateFormatted: LocalDate

    //set the end date to the given date or to the end of the year
    if (endDate == null) {
      endDateFormatted = LocalDate.parse("${startDateFormatted.year}-12-31", dateTimeFormatter)
    } else {
      endDateFormatted = LocalDate.parse(endDate, dateTimeFormatter)
    }

    //from the start to end date, create new shifts
    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy-MM-dd")
    val dateToIncrement = startDate
    calendar.setTime(sdf.parse(dateToIncrement))

    while (startDateFormatted.compareTo(endDateFormatted) <= 0) {
      val newShift = buildShift(
        startTime = startTime,
        endTime = endTime,
        date = startDateFormatted.toString(),
        duration = duration,
        employeeId = employeeId,
        eventId = eventId
      )

      shifts.add(newShift)
      calendar.add(Calendar.DATE, 1)
      startDateFormatted = LocalDate.parse(sdf.format(calendar.getTime()), dateTimeFormatter)
    }

    return shifts
  }

  /**
   * Given a list of shifts, returns the earliest and latest dates for those shifts
   */
  fun getMinAndMaxDatesforEvent(oldShifts: MutableList<Shift>): List<LocalDate> {
    var maxDate: LocalDate = LocalDate.MIN
    var minDate: LocalDate = LocalDate.MAX
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    for (oldShift in oldShifts) {
      val localDate = LocalDate.parse(oldShift.date, dateTimeFormatter)

      if (localDate.compareTo(maxDate) > 0) maxDate = localDate
      if (localDate.compareTo(minDate) < 0) minDate = localDate
    }
    return listOf(minDate, maxDate)
  }

  /**
   * Updates the details of an event and saves it the modified event
   * to the global event list
   */
  fun modifyEventList(
    modifiedEvent: Event,
  ) {
    for (i in this.events.indices) {
      if (modifiedEvent.id == this.events[i].id) {
        this.events[i] = modifiedEvent
      }
    }
  }

  /**
   * We assume the following cases. time moves left to right
   *        start                               end             --> original case
   *                newStart        newEnd                      --> vacation/sick leave
   * newStart                 newEnd                            --> backfill a start date
   *                newStart                        newEnd      --> extend an event
   * Based on these cases,
   * we must check if a new shifts starts before or ends after the start/end dates for existing shifts
   * If it does, we add the override shift to the list and preserve the original shift
   * If a shift falls within the range of the original shift dates, we replace the original shift
   * Return the updated shifts
   */
  fun modifyShiftList(
    oldShifts: MutableList<Shift>,
    newShifts: MutableList<Shift>,
    minDate: LocalDate,
    maxDate: LocalDate,
  ): MutableList<Shift> {

    val updatedShifts = mutableListOf<Shift>()

    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    for (oldShift in oldShifts) {
      var replacedShift = false

      for (newShift in newShifts) {
        //if the dates for this new shift are out of range compared to the existing shifts
        //or if
        if (LocalDate.parse(newShift.date, dateTimeFormatter).compareTo(minDate) < 0
          || LocalDate.parse(newShift.date, dateTimeFormatter).compareTo(maxDate) > 0
        ) {
          updatedShifts.add(newShift)
        } else if (newShift.date == oldShift.date) {
          updatedShifts.add(newShift)
          replacedShift = true
        }
      }
      //if the original event was never replaced, we must add it to the list.
      if (!replacedShift) updatedShifts.add(oldShift)
    }
    return updatedShifts
  }

  /**
   * Prints events in the pattern supplied to the API
   */
  fun printEvents(
    startDate: String,
    dateFormatter: SimpleDateFormat,
    calendar: Calendar,
    numDays: Int,
    printForRange: Boolean = true
  ) {
    var count = 0

    val (year, month, date) = startDate.split("-")
    val endDate = LocalDate.of(year.toInt(), month.toInt(), date.toInt()).plusDays(numDays.toLong())

    val schedule = StringBuilder()
      .append("Printing the schedule for $startDate to $endDate\n")

    //toggle this when there are no events available
    var eventsAvailableForDates = false

    while (count < numDays) {
      val currentDate = dateFormatter.format(calendar.getTime())
      //toggle this when deciding to add current event
      var shiftsPresent = false

      for (event in this.events) {
        //if this event does not have shifts or if there are no shifts on this current date, continue
        //to the next event
        val shifts = this.employeeSchedules.get(event.id) ?: continue
        val shiftOnThisDate = shifts.filter { shift -> shift.date == currentDate }.singleOrNull() ?: continue

        //if the event date has not be added to the string
        if (!shiftsPresent) {
          schedule
            .append("=".repeat(10) + " ")
            .append(currentDate)
            .append(" " + "=".repeat(10) + "\n")
        }
        shiftsPresent = true

        //format the duration ie) 6.5 hrs stays as 6.5 hrs; but 8.0 hrs will become 8 hrs
        val shiftOnThisDateDurationFormatted =
          if (shiftOnThisDate.duration.rem(shiftOnThisDate.duration.toInt()) > 0)
            shiftOnThisDate.duration.toString()
          else shiftOnThisDate.duration.toInt().toString()

        appendDetailsToString(
          schedule = schedule,
          event = event,
          shift = shiftOnThisDate,
          shiftDuration = shiftOnThisDateDurationFormatted
        )
        //flip this to indicate that there is at least one event for the given dates
        eventsAvailableForDates = true
      }

      if (eventsAvailableForDates) schedule.append("\n")
      calendar.add(Calendar.DATE, 1)
      dateFormatter.format(calendar.getTime())
      count += 1
    }

    if (eventsAvailableForDates) {
      println(schedule.toString())
      return
    }
    else if (printForRange) {
      schedule.append("No events in schedule.")
      println(schedule.append("\n\n").toString())
      return
    }
    else{
      println("")
    }
  }

  /**
   * Appends an event's details to a given string for a given date.
   */
  fun appendDetailsToString(
    schedule: StringBuilder,
    event: Event,
    shift: Shift,
    shiftDuration: String
  ): StringBuilder {

    val delimiter = " | "
    return schedule
      .append("Event ${event.id}")
      .append(delimiter)
      .append("Date: ${shift.date}")
      .append(delimiter)
      .append("Shift Time: ${shift.startTime} - ${shift.endTime}")
      .append(delimiter)
      .append("$shiftDuration hrs")
      .append(delimiter)
      .append("Employee ${shift.employeeId}")
      .append("\n")
  }

  /**
   * Given a start time in the "HH:MM" format and a duration,
   * returns a formatted end time in "HH:MM"
   */
  private fun getEndTime(startTime: String, duration: Float): String {
    val (startHours, startMins) = startTime.split(":")
    val durationInMinutes = (duration % duration.toInt() * 60) + (duration.toInt() * 60)

    return LocalTime.of(startHours.toInt(), startMins.toInt())
      .plusMinutes(durationInMinutes.toLong()).toString()
  }

  /**
   * Modifies a time's formatting from "HH:MM"
   * to "HH:MM:SS -$TimeZoneInUTC"
   */
  private fun formatTime(time: String): String {
    val (startHours, startMins) = time.split(":")

    //grab the local UTC offset
    val tz = TimeZone.getDefault()
    val now = Date()
    val offsetFromUTC = Math.abs(tz.getOffset(now.getTime()) / 3600000)
    val formattedUTC = StringBuilder().append(" -0").append("$offsetFromUTC").append(":00")

    val time = listOf(startHours, startMins, "00").joinToString(":") + "$formattedUTC"
    return time
  }
}
