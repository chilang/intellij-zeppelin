package intellij.zeppelin

import com.intellij.notification.{NotificationType, Notifications}
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.{Document, Editor, SelectionModel}
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile


sealed trait SelectionMode
object SelectedText extends SelectionMode
object SingleLine extends SelectionMode

case class CodeFragment(selectionMode: SelectionMode, content:String)

trait IdeaDocumentApi {

  def currentEditor(anActionEvent: AnActionEvent): Editor = {
    FileEditorManagerEx.getInstanceEx(anActionEvent.getProject).getSelectedTextEditor
  }

  def invokeLater(f: => Unit): Unit = {
    ApplicationManager.getApplication.invokeLater(new Runnable {
      override def run(): Unit = f
    })
  }

  def show(message:String): Unit = invokeLater{
    val notification = new com.intellij.notification.Notification(
      "",
      "Zeppelin Idea",
      message,
      NotificationType.INFORMATION,
      null
    )
    ApplicationManager.getApplication.getMessageBus.syncPublisher(Notifications.TOPIC).notify(notification)
  }


  def replaceLine(editor: Editor, line: Int, withText:String): Unit = {
    editor.getDocument.replaceString(
      editor.getDocument.getLineStartOffset(line),
      editor.getDocument.getLineEndOffset(line),
      withText
    )
  }

  def findPreviousLineMatching(editor: Editor, lineMatching:String => Boolean): Option[Int] = {
    val currentLine = editor.getCaretModel.getLogicalPosition.line
    val previousParagraphMarkerLine: Option[Int] = Range(currentLine, 1, -1).map { line =>
      val start = editor.getDocument.getLineStartOffset(line)
      val end = editor.getDocument.getLineEndOffset(line)
      (line, editor.getDocument.getCharsSequence.subSequence(start, end).toString)
    }.collectFirst {
      case (line, text) if lineMatching(text) => line
    }
    previousParagraphMarkerLine
  }

  def currentCodeFragment(editor: Editor): CodeFragment = {
    val text = currentSelectedText(editor)
    if (text.isEmpty) CodeFragment(SingleLine, currentLineText(editor)) else CodeFragment(SelectedText, text)
  }

  def currentLineText(editor: Editor):String = {
    val currentLine = editor.getCaretModel.getLogicalPosition.line
    editor.getDocument.getCharsSequence.subSequence(
      editor.getDocument.getLineStartOffset(currentLine),
        editor.getDocument.getLineEndOffset(currentLine)
    ).toString
  }
  def currentSelectedText(editor: Editor): String = {
    val selectionModel = editor.getSelectionModel
    val blockStarts = selectionModel.getBlockSelectionStarts
    val blockEnds = selectionModel.getBlockSelectionEnds
    editor.getDocument.getCharsSequence.subSequence(blockStarts(0), blockEnds(0)).toString
  }

  def insertAfterFragment(editor: Editor, fragment:CodeFragment, text: String): Unit = {
    editor.getDocument.insertString(lineStartOffsetAfter(editor, fragment), text)
  }

  private def lineStartOffsetAfter(editor: Editor, fragment: CodeFragment): Int = {
    fragment.selectionMode match {
      case SelectedText =>
        val currentLine = editor.getSelectionModel.getSelectionEndPosition.line
        editor.getDocument.getLineEndOffset(currentLine)
      case SingleLine =>
        val currentLine = editor.getCaretModel.getLogicalPosition.line
        editor.getDocument.getLineEndOffset(currentLine)
    }
  }

  def insertBeforeFragment(editor: Editor, fragment: CodeFragment, text: String): Unit = {
    val lineStartOffset = fragment.selectionMode match {
      case SelectedText => editor.getDocument.getLineStartOffset(editor.getSelectionModel.getSelectionStartPosition.line)
      case SingleLine => editor.getDocument.getLineStartOffset(editor.getCaretModel.getLogicalPosition.line)
    }
    editor.getDocument.insertString(lineStartOffset, text)
  }


  def currentDocument(file: VirtualFile): Document = FileDocumentManager.getInstance().getDocument(file)

  def currentFileIn(project: Project): VirtualFile = FileEditorManagerEx.getInstanceEx(project).getCurrentFile
}
