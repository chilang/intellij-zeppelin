package intellij.zeppelin

import java.net.HttpCookie

import spray.json.{JsString, _}

import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}
import scalaj.http.Http

case class Notebook(id:String, size:Int) {
  def notebookHeader(url:String):String = Seq(markerText, s"//$url/#/notebook/$id").mkString("\n")
  def markerText:String = s"//Notebook:$id,$size"
}
case class Paragraph(id:String, index:Int) {
  def markerText:String = s"//Paragraph:$id,$index"
}

case class ParagraphResult(paragraph: Paragraph, results:Seq[String]) {
  def markerText:String = {
    results.flatMap(_.split("\n").map(x => s"\n//$x")).mkString("")
  }
}

object Notebook {
  private val NoteId: Regex = """.*//Notebook:(\w+),(\d+).*""".r
  def parse(text:String):Option[Notebook] = text match {
    case NoteId(id, size) => Some(Notebook(id, size.toInt))
    case _ => None
  }
}

object Paragraph{
  private val ParagraphId: Regex = """.*//Paragraph:([\w_-]+),(\d+).*""".r
  def parse(text:String):Option[Paragraph] = text match {
    case ParagraphId(id, size) => Some(Paragraph(id, size.toInt))
    case _ => None
  }

}
case class Credentials(username:String, password:String)

class ZeppelinApi(val url:String, credentials:Option[Credentials]){

  lazy val sessionToken: Option[HttpCookie] = credentials.flatMap { c =>
    val r = Http(s"$url/api/login").postForm(Seq(
      ("username", c.username),
      ("password", c.password)
    ))
    r.asString.cookies.headOption
  }


  def createNotebook(name:String):Try[Notebook] = {
    val req = request("/api/notebook").postData(
      s"""
        |{"name": "$name"}
      """.stripMargin)

    val response = req.asString.body
    response.parseJson.asJsObject().fields("body") match {
      case JsString(s) => Success(Notebook(s, 0))
      case _ => Failure(new RuntimeException("Error creating new Zeppelin notebook"))
    }
  }

  private def request(path:String) = {
    val r = Http(s"$url$path")
    sessionToken.fold(r)(cookie => r.header("Cookie", s"${cookie.getName}=${cookie.getValue}"))
  }

  def createParagraph(notebook: Notebook, text: String, atIndex:Option[Int] = None):Try[Paragraph] = {
    val escaped = text.replaceAll("\\\"", "\\\\\"")

    val body = atIndex match {
      case Some(index) => s"""{"title":"new note", "text": "$escaped", "index": $index}"""
      case None => s"""{"title":"new note", "text": "$escaped"}"""
    }
    val req = request(s"/api/notebook/${notebook.id}/paragraph").postData(body)

    req.asString.body.parseJson.asJsObject.fields("body") match {
      case JsString(paragraphId) => Success(Paragraph(paragraphId, notebook.size))
      case _ => Failure(new RuntimeException("Error creating new Zeppelin paragraph"))
    }
  }

  def deleteParagraph(notebook: Notebook, paragraph: Paragraph):Try[Paragraph] = {
    val result = request(s"/api/notebook/${notebook.id}/paragraph/${paragraph.id}").method("DELETE")
      .asString
      .body.parseJson.asJsObject

    result.fields("status") match {
      case JsString(status) if status == "OK" => Success(paragraph)
      case unknown => Failure(new RuntimeException(s"Unrecognized response $unknown"))
    }
  }

  def replaceParagraph(notebook: Notebook, paragraph: Paragraph, text:String): Try[Paragraph] = {
    deleteParagraph(notebook, paragraph).flatMap(_ => createParagraph(notebook, text, Some(paragraph.index)))
  }

  def runParagraph(notebook: Notebook, paragraph: Paragraph):Try[ParagraphResult] = {
    val result = request(s"/api/notebook/run/${notebook.id}/${paragraph.id}").postData("")
      .asString
      .body.parseJson.asJsObject

    result.fields("status") match {
      case JsString(status) if status == "OK" =>
        result.fields("body").asJsObject.fields("msg") match {
          case JsArray(arr) => Success(arr.map(_.asJsObject.fields("data"))
            .collect { case JsString(s) => s })
            .map(ParagraphResult(paragraph, _))
          case other => Failure(new RuntimeException(s"Unrecognized result $other"))
        }
      case unknown => Failure(new RuntimeException(s"Unrecognized response $unknown"))
    }
  }
}

