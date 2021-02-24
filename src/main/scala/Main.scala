//import requests
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Calendar

import upickle._
import upickle.default._
import upickle.default.{ReadWriter => RW, macroRW}

case class Workspace(id: String)
object Workspace{
  implicit val rw: RW[Workspace] = macroRW
}

case class SummaryFilter(groups: List[String])
object SummaryFilter{
  implicit val rw: RW[SummaryFilter] = macroRW
}

case class ReportRequest(dateRangeStart: String, dateRangeEnd: String, summaryFilter: SummaryFilter)
object ReportRequest{
  implicit val rw: RW[ReportRequest] = macroRW
}

case class ChildChild(name: String, duration: Int)
object ChildChild{
  implicit val rw: RW[ChildChild] = macroRW
}

case class Child(duration: Int, name: String, clientName: String, children: List[ChildChild])
object Child{
  implicit val rw: RW[Child] = macroRW
}

case class GroupOne(children: List[Child])
object GroupOne{
  implicit val rw: RW[GroupOne] = macroRW
}

case class ReportResponse(groupOne: List[GroupOne])
object ReportResponse{
  implicit val rw: RW[ReportResponse] = macroRW
}

object Main extends App {
  def clockifyDurationToString(seconds: Int) = {
    val hours = (seconds / 60 / 60) % 24
    val minutes = (seconds / 60) % 60
    val secondsMod = seconds % 60
    s"${hours}h ${minutes}m ${secondsMod}s"
  }

  def listWorkspaces() = {
    val rw = requests.get("https://api.clockify.me/api/v1/workspaces", headers = Map("X-Api-Key" -> ApiKey))
    println(rw.statusCode)
    println(rw.text)

    val d = read[List[Workspace]](rw.text)
    println(d)
    for (x <- d) {
      println(x.id)
    }
  }

  val ApiKey = sys.env("API_KEY")

  // listWorkspaces // if need, uncomment this

  val format = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val startDate = LocalDate.parse(sys.env("START_DATE"), format)
  val endDate = LocalDate.parse(sys.env("END_DATE"), format)
  var date = startDate
  assert(startDate.isBefore(endDate))

  while(date.isBefore(endDate)) {
    date = date.plusDays(1)
    val dayOfWeekFormatter = DateTimeFormatter.ofPattern("E")
    println("-------------------------------------------------------")
    println(s"${date.format(dayOfWeekFormatter)}, ${date.format(format)}")
    val formattedDate = date.format(format)

    val startDate = s"${formattedDate}T00:00:00.000"
    val endDate = s"${formattedDate}T23:59:59.000"

    val workspaceId = sys.env("WORKSPACE_ID")
    val r = requests.post(s"https://reports.api.clockify.me/v1/workspaces/$workspaceId/reports/summary",
      headers = Map("X-Api-Key" -> ApiKey, "content-type" -> "application/json"),
      data = upickle.default.stream(ReportRequest(startDate, endDate, SummaryFilter(List(
            "USER",
            "PROJECT",
            "TIMEENTRY"
            )))))

    val resp = read[ReportResponse](r.text)
    resp.groupOne.headOption.map { case head =>
      for(child <- head.children) {
        println("-----------------------")
        println(s"Client: ${child.clientName}")
        println(s"Project: ${child.name}")
        println(s"Total Duration: ${clockifyDurationToString(child.duration)}")
        child.children.zipWithIndex.foreach { case (item, index) =>
          println(s"[$index] ${item.name} [${clockifyDurationToString(item.duration)}]")
        }

        println("-----------------------")
      }
    }
  }
}
