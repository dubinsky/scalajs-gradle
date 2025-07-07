package org.podval.tools.scalanative

import org.gradle.process.{ExecOperations, ExecSpec}
import org.podval.tools.build.TestEnvironment
import org.podval.tools.nonjvm.{NonJvmTestEnvironment, Run}
import org.podval.tools.platform.OutputPiper
import java.io.File
import scala.scalanative.testinterface.adapter.TestAdapter

final class ScalaNativeRun(
  binaryTestFile: File,
  logSource: String
) extends ScalaNativeBuild(
  logSource
) with Run[ScalaNativeBackend.type]:
  override def run(
    execOperations: ExecOperations,
    outputPiper: OutputPiper
  ): Unit =
    val running: String = binaryTestFile.getAbsolutePath
    execOperations.exec: (exec: ExecSpec) =>
      exec.setCommandLine(running)
      outputPiper.run(running)(outputPiper.start(exec))

  def testEnvironment: TestEnvironment[ScalaNativeBackend.type] =
    NonJvmTestEnvironment[ScalaNativeBackend.type, TestAdapter](
      backend = ScalaNativeBackend,
      testAdapter = TestAdapter(TestAdapter
        .Config()
        .withLogger(backendLogger)
        .withBinaryFile(binaryTestFile)
      ),
      loadFrameworksFromTestAdapter = _.loadFrameworks(_),
      closeTestAdapter = _.close(),
      sourceMapper = None
    )
