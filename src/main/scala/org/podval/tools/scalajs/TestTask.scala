package org.podval.tools.scalajs

import org.opentorah.util.Files
import org.podval.tools.testing.framework.FrameworkDescriptor
import org.podval.tools.testing.task.{SourceMapper, TestEnvironment, TestTask}
import org.scalajs.testing.adapter.TestAdapter
import sbt.testing.Framework

abstract class TestTask extends org.podval.tools.testing.task.TestTask with AfterLinkTask:
  final override protected def flavour: String = "Test"
  final override protected def linkTaskClass: Class[LinkTask.Test] = classOf[LinkTask.Test]

  // Note: ScalaJS tests are not forkable; see org.scalajs.sbtplugin.ScalaJSPluginInternal
  final override protected def canFork: Boolean = false

  final override def sourceMapper: Option[SourceMapper] = createAfterLink
    .mainModule
    .sourceMapName
    .map((name: String) => Files.file(directory = linkTask.getJSDirectory, segments = name))
    .map(ClosureCompilerSourceMapper(_))

  final override def testEnvironment: TestEnvironment =
    val afterLink: AfterLink = createAfterLink

    val testAdapter: TestAdapter = TestAdapter(
      jsEnv = afterLink.jsEnv,
      input = Seq(afterLink.input),
      config = TestAdapter.Config().withLogger(afterLink.jsLogger)
    )

    new TestEnvironment:
      override def loadFrameworks(descriptors: List[FrameworkDescriptor]): List[Framework] = testAdapter
        .loadFrameworks(descriptors.map((descriptor: FrameworkDescriptor) => List(descriptor.implementationClassName)))
        .flatten

      override def close(): Unit =
        testAdapter.close()
