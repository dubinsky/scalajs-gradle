package org.podval.tools.scalajs

import org.opentorah.util.Files
import org.podval.tools.testing.framework.FrameworkDescriptor
import org.podval.tools.testing.task.{SourceMapper, TestEnvironment}
import org.scalajs.testing.adapter.TestAdapter
import sbt.testing.Framework
import java.io.File

abstract class TestTask extends org.podval.tools.testing.task.TestTask with AfterLinkTask:
  final override protected def flavour: String = "Test"
  final override protected def linkTaskClass: Class[LinkTask.Test] = classOf[LinkTask.Test]

  // Note: ScalaJS tests are not forkable; see org.scalajs.sbtplugin.ScalaJSPluginInternal
  final override protected def canFork: Boolean = false

  final override protected def sourceMapper: Option[SourceMapper] = afterLink
    .mainModule
    .sourceMapName
    .map((name: String) => Files.file(directory = linkTask.getJSDirectory, segments = name))
    .map(ClosureCompilerSourceMapper(_))

  final override protected def testEnvironment: TestEnvironment = new TestEnvironment:
    private val testAdapter: TestAdapter = TestAdapter(
      jsEnv = afterLink.jsEnv,
      input = Seq(afterLink.input),
      config = TestAdapter.Config().withLogger(afterLink.jsLogger)
    )

    override def loadFrameworks(testClassPath: Iterable[File]): List[Framework] = testAdapter
      .loadFrameworks(FrameworkDescriptor.all.map((descriptor: FrameworkDescriptor) => List(descriptor.implementationClassName)))
      .flatten

    override def close(): Unit =
      testAdapter.close()
