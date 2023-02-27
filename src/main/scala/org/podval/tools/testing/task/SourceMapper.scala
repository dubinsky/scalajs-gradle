package org.podval.tools.testing.task

import org.gradle.api.internal.tasks.testing.{DefaultTestFailure, DefaultTestFailureDetails}
import org.gradle.api.tasks.testing.{TestExecutionException, TestFailure}
import org.gradle.internal.serialize.PlaceholderExceptionSupport
import org.opentorah.util.Strings

abstract class SourceMapper:
  import SourceMapper.Mapping

  protected def getMapping(line: Int, column: Int): Option[Mapping]

  final def sourceMap(testFailure: TestFailure): TestFailure =
    val throwable: Throwable = testFailure.getRawFailure

    val mappings: Array[(Option[Mapping], StackTraceElement)] =
      for stackTraceElement: StackTraceElement <- throwable.getStackTrace yield
        val mapping: Option[Mapping] = getMapping(stackTraceElement.getLineNumber, 1)

        val newStackTraceElement: StackTraceElement = mapping.fold(stackTraceElement)((mapping: Mapping) =>
          StackTraceElement(
            stackTraceElement.getClassLoaderName,
            stackTraceElement.getModuleName,
            stackTraceElement.getModuleVersion,
            stackTraceElement.getClassName,
            stackTraceElement.getMethodName,
            mapping.file,
            mapping.line
          ))

        mapping -> newStackTraceElement

    val location: String = mappings
      .flatMap(_._1)
      .find(_.file.startsWith("file://"))
      .fold("") { (mapping: Mapping) =>
        val filePath: String = Strings.drop(mapping.file, "file://")
        s" ($filePath:${mapping.line}:${mapping.column})"
      }

    val message: String = Option(testFailure.getDetails.getMessage).getOrElse(s"$throwable was thrown")

    // TODO do not wrap?
    val throwableMapped: TestExecutionException = new TestExecutionException(
      s"$message$location",
      throwable.getCause
    )

    throwableMapped.setStackTrace(mappings.map(_._2))

    val details = testFailure.getDetails

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
        details.getExpected,
        details.getActual
      ),
      testFailure.getCauses
    )

object SourceMapper:
  final class Mapping(
    val file: String,
    val line: Int,
    val column: Int
  )

  private def stacktraceOf(throwable: Throwable): String =
    try
      val out = java.io.StringWriter()
      throwable.printStackTrace(java.io.PrintWriter(out))
      out.toString
    catch
      case t: Exception => stacktraceOf(t)
