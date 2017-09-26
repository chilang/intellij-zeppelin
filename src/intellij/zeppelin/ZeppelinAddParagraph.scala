package intellij.zeppelin

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor

import scala.util.Try

class ZeppelinAddParagraph extends ZeppelinAction {

  override def actionPerformed(anActionEvent: AnActionEvent): Unit = {

    val editor = currentEditor(anActionEvent)
    val api = zeppelin(anActionEvent)
    findNotebook(editor)
      .map { notebook =>
        val codeFragment = currentCodeFragment(editor)
        (for {
          paragraph <- api.createParagraph(notebook, codeFragment.content)
          _ <- Try(runWriteAction(anActionEvent){ _ =>

            updateNotebookMarker(editor, notebook.copy(size = notebook.size+1))
            insertBeforeFragment(editor, codeFragment, paragraph.markerText + "\n")
          })
          result <- api.runParagraph(notebook, paragraph)
        } yield {
          runWriteAction(anActionEvent) { _ =>
            insertAfterFragment(editor, codeFragment, result.markerText)
          }
        }).recover { case t: Throwable => show(t.toString) }
      }.getOrElse(show("No Zeppelin NoteId found."))

  }

  private def updateNotebookMarker(editor: Editor, notebook: Notebook): Unit = {
    findPreviousLineMatching(editor, text => Notebook.parse(text).isDefined).foreach { line =>
      replaceLine(editor, line, notebook.markerText)
    }
  }


}


