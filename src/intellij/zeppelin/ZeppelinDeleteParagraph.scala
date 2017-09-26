package intellij.zeppelin

import com.intellij.openapi.actionSystem.AnActionEvent

class ZeppelinDeleteParagraph extends ZeppelinAction {

  override def actionPerformed(anActionEvent: AnActionEvent): Unit = {

    val editor = currentEditor(anActionEvent)
    (for {
      notebook <- findNotebook(editor)
      paragraph <- findParagraph(editor)
    } yield {
      (for {
         _ <- zeppelin(anActionEvent).deleteParagraph(notebook, paragraph)
       } yield {
         runWriteAction(anActionEvent){ _ =>
           findPreviousLineMatching(editor, line => Paragraph.parse(line).isDefined).foreach{ line =>
              replaceLine(editor, line, "")
           }
         }
       }).recover { case t: Throwable => show(t.toString) }
      }).getOrElse(show("No Zeppelin NoteId found."))

  }

}


