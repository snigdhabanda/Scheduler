data class Event(
  val id: Int,
  val startTime: String,
  val endTime: String,
  val duration: Float
)

data class Employee(
  val id: Int,
)

data class Shift(
  val eventId: Int,
  val employeeId: Int,
  val startTime: String,
  val endTime: String,
  val duration: Float,
  val date: String
)
