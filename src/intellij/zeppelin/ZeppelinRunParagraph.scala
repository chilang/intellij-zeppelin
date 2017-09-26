package intellij.zeppelin

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor

class ZeppelinRunParagraph extends ZeppelinAction {

  override def actionPerformed(anActionEvent: AnActionEvent): Unit = {
    val editor = currentEditor(anActionEvent)
    val api = zeppelin(anActionEvent)
    for {
      note <- findNotebook(editor)
      paragraph <- findParagraph(editor)
    } yield {

      val codeFragment = currentCodeFragment(editor)

      (for {
          newParagraph <- api.replaceParagraph(note, paragraph, codeFragment.content)
          result <- api.runParagraph(note, newParagraph)
        } yield {
          runWriteAction(anActionEvent) { _ =>
            replaceParagraphMarker(editor, paragraph, newParagraph)
            insertAfterFragment(editor, codeFragment, result.markerText)
          }
        }).recover { case t: Throwable => show(t.toString) }
      }.getOrElse(show("No Zeppelin //Notebook: marker found."))

  }

  private def replaceParagraphMarker(editor: Editor, existingParagraph: Paragraph, newParagraph: Paragraph): Unit = {
    findPreviousLineMatching(editor, text => Paragraph.parse(text).isDefined).foreach { line =>
      replaceLine(editor, line, newParagraph.markerText)
    }
  }

}


