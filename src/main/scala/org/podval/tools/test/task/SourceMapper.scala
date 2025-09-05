package org.podval.tools.test.task

import org.gradle.api.internal.tasks.testing.{AssertionFailureDetails, DefaultTestFailure}
import org.gradle.api.tasks.testing.{TestExecutionException, TestFailure, TestFailureDetails}
import org.gradle.internal.serialize.PlaceholderExceptionSupport
import org.podval.tools.platform.Strings
import scala.annotation.tailrec

abstract class SourceMapper:
  protected def getMapping(line: Int, column: Int): Option[SourceMapper.SourceLocation]

  final def sourceMap(testFailure: TestFailure): TestFailure =
    val throwable: Throwable = testFailure.getRawFailure

    val mappings: Array[(Option[SourceMapper.SourceLocation], StackTraceElement)] =
      for stackTraceElement: StackTraceElement <- throwable.getStackTrace yield
        val mapping: Option[SourceMapper.SourceLocation] = getMapping(stackTraceElement.getLineNumber, 1)

        val newStackTraceElement: StackTraceElement = mapping.fold(stackTraceElement): (sourceLocation: SourceMapper.SourceLocation) =>
          StackTraceElement(
            stackTraceElement.getClassLoaderName,
            stackTraceElement.getModuleName,
            stackTraceElement.getModuleVersion,
            stackTraceElement.getClassName,
            stackTraceElement.getMethodName,
            sourceLocation.file,
            sourceLocation.line
          )

        mapping -> newStackTraceElement

    val location: String = mappings
      .flatMap(_._1)
      .find(_.file.startsWith("file://"))
      .fold(""): (sourceLocation: SourceMapper.SourceLocation) =>
        val filePath: String = Strings.dropPrefix(sourceLocation.file, "file://")
        s" ($filePath:${sourceLocation.line}:${sourceLocation.column})"

    val details: TestFailureDetails = testFailure.getDetails
    val message: String = Option(details.getMessage).getOrElse(s"$throwable was thrown")
    val throwableMapped: Throwable = TestExecutionException(s"$message$location", throwable.getCause)
    throwableMapped.setStackTrace(mappings.map(_._2))

    // Based on `DefaultTestFailure`.
    DefaultTestFailure(
      throwableMapped,
      AssertionFailureDetails(
        message,
        throwable match
          case e: PlaceholderExceptionSupport => e.getExceptionClassName
          case _ => throwable.getClass.getName,
        SourceMapper.stacktraceOf(throwableMapped),
        details.getExpected,
        details.getActual
      ),
      testFailure.getCauses
    )

object SourceMapper:
  final class SourceLocation(
    val file: String,
    val line: Int,
    val column: Int
  )

  @tailrec
  private def stacktraceOf(throwable: Throwable): String =
    try
      val out = java.io.StringWriter()
      throwable.printStackTrace(java.io.PrintWriter(out))
      out.toString
    catch
      case t: Exception => stacktraceOf(t)
