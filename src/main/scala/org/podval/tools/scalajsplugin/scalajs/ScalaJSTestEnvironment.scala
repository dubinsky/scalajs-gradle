package org.podval.tools.scalajsplugin.scalajs

import org.podval.tools.build.ScalaBackendKind
import org.podval.tools.scalajs.ScalaJSRunCommon
import org.podval.tools.test.environment.TestEnvironment
import org.podval.tools.test.framework.FrameworkDescriptor
import org.podval.tools.util.Files
import org.scalajs.testing.adapter.TestAdapter
import sbt.testing.Framework

final class ScalaJSTestEnvironment(runCommon: ScalaJSRunCommon) extends TestEnvironment:
  private val testAdapter: TestAdapter = runCommon.createTestAdapter

  override def close(): Unit = testAdapter.close()

  override protected def expandClassPath: Boolean = false
  
  override protected def loadFrameworks: List[Framework] = testAdapter
    .loadFrameworks(FrameworkDescriptor
      .forBackend(ScalaBackendKind.JS)
      .map((descriptor: FrameworkDescriptor) => List(descriptor.className))
    )
    .flatten

  override def sourceMapper: Option[ClosureCompilerSourceMapper] = runCommon
    .mainModule
    .sourceMapName
    .map((name: String) => Files.file(runCommon.common.jsDirectory, name))
    .map(ClosureCompilerSourceMapper(_))
