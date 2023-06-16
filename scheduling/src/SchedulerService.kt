
fun main(){
  val schedulerApi = RealSchedulerApi()
  schedulerApi.scheduleEventForEmployee(1, "2023-03-01", null, "08:00", 8F) // create an Event with ID = 1
  schedulerApi.scheduleEventForEmployee(8, "2023-03-01", "2023-03-30", "06:00", 7F)  // creates Event with ID = 2

  schedulerApi.scheduleOverrideForEvent(1, "TODAY_FORWARD", 4, "2023-03-14", "2023-05-30", null, null)
  schedulerApi.scheduleOverrideForEvent(1, "TODAY_FORWARD", 6, "2023-03-14", null, "10:00", null)
  schedulerApi.scheduleOverrideForEvent(1, "TODAY_FORWARD", 7, "2023-03-21", "2023-04-10", null, 6.5F)
  schedulerApi.scheduleOverrideForEvent(1, "TODAY_FORWARD", 8, "2023-04-01", null, null, null)
  schedulerApi.scheduleOverrideForEvent(1, "TODAY_FORWARD", 4, "2023-04-01", "2023-04-15", "07:00", null)

  schedulerApi.scheduleOverrideForEvent(2, "TODAY_ONLY", 2, "2023-03-07", null, null, null)
  schedulerApi.scheduleOverrideForEvent(2, "TODAY_ONLY", 2, "2023-03-08", null, null, null)
  schedulerApi.scheduleOverrideForEvent(2, "TODAY_ONLY", 3, "2023-03-08", null, "05:00", 8F)
  schedulerApi.scheduleOverrideForEvent(2, "TODAY_ONLY", 3, "2023-03-10", null, null, 9F)
  schedulerApi.scheduleOverrideForEvent(2, "TODAY_ONLY", 3, "2023-03-14", null, "09:00", 4.5F)

  schedulerApi.printEventsForRange("2023-02-04", 4)
  schedulerApi.printEventsForRange("2023-03-05", 6)
  schedulerApi.printEventsForRange("2023-02-27", 4)
  schedulerApi.printEventsForRange("2023-03-25", 10)

  schedulerApi.printFullSchedule()
}
