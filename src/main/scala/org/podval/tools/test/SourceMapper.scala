package org.podval.tools.test

import org.gradle.api.tasks.testing.TestExecutionException
import org.opentorah.util.Strings

abstract class SourceMapper:
  import SourceMapper.Mapping

  protected def getMapping(line: Int, column: Int): Option[Mapping]

  final def sourceMap(throwable: Throwable): Throwable =
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

    val message: String = Option(throwable.getMessage).getOrElse(s"$throwable was thrown")

    val result: TestExecutionException = new TestExecutionException(
      s"$message$location",
      throwable.getCause
    )

    result.setStackTrace(mappings.map(_._2))

    result

object SourceMapper:
  final class Mapping(
    val file: String,
    val line: Int,
    val column: Int
  )
