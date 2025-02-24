package org.podval.tools.scalajs

import org.podval.tools.testing.TestEnvironment
import org.podval.tools.testing.framework.FrameworkDescriptor
import org.podval.tools.util.Files
import org.scalajs.testing.adapter.TestAdapter
import sbt.testing.Framework
import java.io.File

final class ScalaJSTest(common: ScalaJSRunCommon):
  def sourceMapper: Option[ClosureCompilerSourceMapper] = common.mainModule
    .sourceMapName
    .map((name: String) => Files.file(common.common.jsDirectory, name))
    .map(ClosureCompilerSourceMapper(_))

  def testEnvironment: TestEnvironment = new TestEnvironment:
    private val testAdapter: TestAdapter = TestAdapter(
      jsEnv = common.mkJsEnv,
      input = Seq(common.input),
      config = TestAdapter.Config().withLogger(common.common.jsLogger)
    )

    override def loadFrameworks(testClassPath: Iterable[File]): List[Framework] = testAdapter
      .loadFrameworks(FrameworkDescriptor
        .scalaJSSupported
        .map((descriptor: FrameworkDescriptor) => List(descriptor.className))
      )
      .flatten

    override def close(): Unit =
      testAdapter.close()
