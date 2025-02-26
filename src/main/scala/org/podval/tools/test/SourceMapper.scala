package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.{DefaultTestFailure, DefaultTestFailureDetails}
import org.gradle.api.tasks.testing.{TestExecutionException, TestFailure, TestFailureDetails}
import org.gradle.internal.serialize.PlaceholderExceptionSupport
import org.podval.tools.util.Strings
import scala.annotation.tailrec

abstract class SourceMapper:
  import SourceMapper.Mapping

  protected def getMapping(line: Int, column: Int): Option[Mapping]

  final def sourceMap(testFailure: TestFailure): TestFailure =
    val throwable: Throwable = testFailure.getRawFailure

    val mappings: Array[(Option[Mapping], StackTraceElement)] =
      for stackTraceElement: StackTraceElement <- throwable.getStackTrace yield
        val mapping: Option[Mapping] = getMapping(stackTraceElement.getLineNumber, 1)

        val newStackTraceElement: StackTraceElement = mapping.fold(stackTraceElement): (mapping: Mapping) =>
          StackTraceElement(
            stackTraceElement.getClassLoaderName,
            stackTraceElement.getModuleName,
            stackTraceElement.getModuleVersion,
            stackTraceElement.getClassName,
            stackTraceElement.getMethodName,
            mapping.file,
            mapping.line
          )

        mapping -> newStackTraceElement

    val location: String = mappings
      .flatMap(_._1)
      .find(_.file.startsWith("file://"))
      .fold(""): (mapping: Mapping) =>
        val filePath: String = Strings.drop(mapping.file, "file://")
        s" ($filePath:${mapping.line}:${mapping.column})"

    val details: TestFailureDetails = testFailure.getDetails
    val message: String = Option(details.getMessage).getOrElse(s"$throwable was thrown")

    // Gradle's org.gradle.internal.serialize.ExceptionPlaceholder serializes exceptions with lots of details;
    // org.scalajs.testing.common.Serializer.ThrowableSerializer - not so much: they all become
    // "org.scalajs.testing.common.Serializer$ThrowableSerializer$$anon$3";
    // since source mapping is used only for ScalaJS, there is no point trying to preserve the original
    // exception here: it is already lost...
    // So, I might as well just wrap what remains in:
    val throwableMapped: Throwable = TestExecutionException(s"$message$location", throwable.getCause)
    throwableMapped.setStackTrace(mappings.map(_._2))

    // Note: copied stuff from DefaultTestFailure
    DefaultTestFailure(
      throwableMapped,
      DefaultTestFailureDetails(
        message,
        throwable match
          case e: PlaceholderExceptionSupport => e.getExceptionClassName
          case _ => throwable.getClass.getName,
        SourceMapper.stacktraceOf(throwableMapped),
        details.isAssertionFailure,
        details.isFileComparisonFailure,
        details.getExpected,
        details.getActual,
        details.getExpectedContent,
        details.getActualContent
      ),
      testFailure.getCauses
    )

object SourceMapper:
  final class Mapping(
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
