package org.podval.tools.scalajsplugin.scalanative

import org.podval.tools.build.ScalaBackendKind
import org.podval.tools.scalanative.ScalaNativeBuild
import org.podval.tools.test.environment.{SourceMapper, TestEnvironment}
import org.podval.tools.test.framework.FrameworkDescriptor
import sbt.testing.Framework
import scala.scalanative.testinterface.adapter.TestAdapter
import java.io.File

final class ScalaNativeTestEnvironment(binaryTestFile: File) extends TestEnvironment:
  private val testAdapter: TestAdapter = ScalaNativeBuild.createTestAdapter(binaryTestFile)

  override def close(): Unit = testAdapter.close()

  override protected def expandClassPath: Boolean = false

  override def sourceMapper: Option[SourceMapper] = None // TODO

  override protected def loadFrameworks: List[Framework] = testAdapter
    .loadFrameworks(FrameworkDescriptor
      .forBackend(ScalaBackendKind.Native)
      .map((descriptor: FrameworkDescriptor) => List(descriptor.className))
    )
    .flatten
