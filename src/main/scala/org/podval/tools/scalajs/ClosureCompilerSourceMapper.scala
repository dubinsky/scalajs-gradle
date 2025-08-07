package org.podval.tools.scalajs

import com.google.debugging.sourcemap.SourceMapConsumerV3
import com.google.debugging.sourcemap.proto.Mapping.OriginalMapping
import org.podval.tools.platform.Files
import org.podval.tools.test.task.SourceMapper
import java.io.File

final class ClosureCompilerSourceMapper(sourceMapFile: File) extends SourceMapper:
  private var consumer: Option[SourceMapConsumerV3] = None

  override protected def getMapping(line: Int, column: Int): Option[SourceMapper.SourceLocation] =
    if consumer.isEmpty then
      val result: SourceMapConsumerV3 = new SourceMapConsumerV3
      result.parse(Files.read(sourceMapFile).mkString("\n"))
      consumer = Some(result)

    Option(consumer.get.getMappingForLine(line, column))
      .map(ClosureCompilerSourceMapper.toSourceLocation)

object ClosureCompilerSourceMapper:
  private def toSourceLocation(result: OriginalMapping): SourceMapper.SourceLocation = SourceMapper.SourceLocation(
    file = result.getOriginalFile,
    line = result.getLineNumber,
    column = result.getColumnPosition
  )
