package org.podval.tools.scalajsplugin.scalajs

import org.podval.tools.scalajs.ScalaJSRunCommon
import org.podval.tools.test.environment.TestEnvironment
import org.podval.tools.test.framework.FrameworkDescriptor
import org.podval.tools.util.Files
import org.scalajs.testing.adapter.TestAdapter
import org.slf4j.Logger
import sbt.testing.Framework
import java.io.File

final class ScalaJSTestEnvironment(runCommon: ScalaJSRunCommon) extends TestEnvironment:
  private val testAdapter: TestAdapter = TestAdapter(
    jsEnv = runCommon.mkJsEnv,
    input = Seq(runCommon.input),
    config = TestAdapter.Config().withLogger(runCommon.common.loggerJS)
  )

  override def close(): Unit = testAdapter.close()

  protected def expandClassPath: Boolean = false
  
  override protected def loadFrameworks: List[Framework] = testAdapter
    .loadFrameworks(FrameworkDescriptor.scalaJSSupported.map((descriptor: FrameworkDescriptor) => List(descriptor.className)))
    .flatten

  override def sourceMapper: Option[ClosureCompilerSourceMapper] = runCommon
    .mainModule
    .sourceMapName
    .map((name: String) => Files.file(runCommon.common.jsDirectory, name))
    .map(ClosureCompilerSourceMapper(_))
