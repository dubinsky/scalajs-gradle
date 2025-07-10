package org.podval.tools.scalanative

import org.podval.tools.build.TestEnvironment
import org.podval.tools.nonjvm.{NonJvmTestEnvironment, Run}
import org.podval.tools.platform.Runner
import java.io.File
import scala.scalanative.testinterface.adapter.TestAdapter

final class ScalaNativeRun(
  binaryTestFile: File,
  logSource: String
) extends ScalaNativeBuild(
  logSource
) with Run[ScalaNativeBackend.type]:

  override def run(runner: Runner): Unit = runner.exec(_.setCommandLine(binaryTestFile.getAbsolutePath))

  override def testEnvironment: TestEnvironment[ScalaNativeBackend.type] =
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
