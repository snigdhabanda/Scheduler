interface SchedulerApi {
  val scheduler: Scheduler

  fun scheduleEventForEmployee(
    employeeId: Int,
    startDate: String,
    endDate: String?,
    startTime: String,
    duration: Float,
  ): Boolean

  fun scheduleOverrideForEvent(
    eventId: Int,
    overrideType: String,
    overrideEmployeeId: Int,
    overrideStartDate: String,
    newEndDate: String?,
    newStartTime: String?,
    newDuration: Float?,
  ) : Boolean

  fun printEventsForRange(
    startDate: String,
    numDays: Int,
  )

  fun printFullSchedule()

}
