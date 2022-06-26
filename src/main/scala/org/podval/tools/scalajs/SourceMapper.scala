package org.podval.tools.scalajs

import com.google.debugging.sourcemap.SourceMapConsumerV3
import com.google.debugging.sourcemap.proto.Mapping
import org.opentorah.util.Strings
import java.io.File

final class SourceMapper(sourceMapFile: Option[File], projectRootFile: File):
  private var consumer: Option[SourceMapConsumerV3] = None

  private def getConsumer: Option[SourceMapConsumerV3] =
    if consumer.isEmpty && sourceMapFile.isDefined then
      val result: SourceMapConsumerV3 = new SourceMapConsumerV3
      result.parse(sbt.io.IO.read(sourceMapFile.get))
      consumer = Some(result)
    consumer

  def stackTraceMessage(t: Throwable): String =
    getConsumer.fold("TODO: un-source-mapped stack trace")((consumer: SourceMapConsumerV3) => t.getStackTrace
      .map((stackTraceElement: StackTraceElement) =>
        consumer.getMappingForLine(stackTraceElement.getLineNumber, 1)
      )
      .filter(_.getOriginalFile.startsWith("file://"))
      .map { (mapping: Mapping.OriginalMapping) =>
        val fileUrl: String = mapping.getOriginalFile
        val filePath = Strings.drop(fileUrl, s"file://")
//        val filePath = Strings.drop(fileUrl, s"file://${projectRootFile.getAbsolutePath}")
        s"$filePath:${mapping.getLineNumber}:${mapping.getColumnPosition}"
      }
      .mkString("\n")
    )
