package org.podval.tools.scalajs

import com.google.debugging.sourcemap.SourceMapConsumerV3
import org.opentorah.util.Files
import org.podval.tools.testing.task.SourceMapper
import java.io.File

final class ClosureCompilerSourceMapper(sourceMapFile: File) extends SourceMapper:
  private var consumer: Option[SourceMapConsumerV3] = None

  override protected def getMapping(line: Int, column: Int): Option[SourceMapper.Mapping] =
    if consumer.isEmpty then
      val result: SourceMapConsumerV3 = new SourceMapConsumerV3
      result.parse(Files.read(sourceMapFile).mkString("\n"))
      consumer = Some(result)

    consumer.fold(None)((consumer: SourceMapConsumerV3) =>
      Option(consumer.getMappingForLine(line, column))
        .map((result: com.google.debugging.sourcemap.proto.Mapping.OriginalMapping) =>
          SourceMapper.Mapping(
            file = result.getOriginalFile,
            line = result.getLineNumber,
            column = result.getColumnPosition
          )
        )
    )
