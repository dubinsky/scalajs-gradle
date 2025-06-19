package org.podval.tools.scalanative

import org.gradle.process.{ExecOperations, ExecSpec}
import org.podval.tools.platform.OutputPiper

import java.io.File
import scala.scalanative.testinterface.adapter.TestAdapter

final class ScalaNativeRun(
  binaryTestFile: File,
  logSource: String
) extends ScalaNativeBuild(
  logSource
):
  def run(
    execOperations: ExecOperations,
    outputPiper: OutputPiper
  ): Unit =
    val running: String = binaryTestFile.getAbsolutePath
    execOperations.exec: (exec: ExecSpec) =>
      exec.setCommandLine(running)
      outputPiper.run(running)(outputPiper.start(exec))

  def createTestEnvironment: ScalaNativeBackend.TestEnvironment =
    ScalaNativeBackend.createTestEnvironment[TestAdapter](
      testAdapter = TestAdapter(TestAdapter
        .Config()
        .withLogger(loggerN)
        .withBinaryFile(binaryTestFile)
      ),
      loadFrameworksFromTestAdapter = _.loadFrameworks(_),
      closeTestAdapter = _.close(),
      sourceMapperOpt = None
    )
