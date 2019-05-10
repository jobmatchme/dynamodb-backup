package eu.jobmatchme.dynamodb.backup

import java.io.{ PrintWriter, StringWriter }


object StackTraceUtil {
  def getStackTrace(throwable: Throwable): String = {
    val stringWriter = new StringWriter
    val printWriter = new PrintWriter(stringWriter)
    throwable.printStackTrace(printWriter)
    val stackTrace = stringWriter.toString()
    stringWriter.close()
    printWriter.close()
    stackTrace
  }
}
