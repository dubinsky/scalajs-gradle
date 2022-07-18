package org.podval.tools.scalajs

import org.opentorah.util.Files
import org.podval.tools.test.{FrameworkDescriptor, SourceMapper, TestEnvironment}
import org.scalajs.testing.adapter.TestAdapter
import sbt.testing.Framework
import java.io.File

abstract class TestTask extends org.podval.tools.test.TestTask with AfterLinkTask:
  final override protected def flavour: String = "Test"
  final override protected def linkTaskClass: Class[LinkTask.Test] = classOf[LinkTask.Test]

  // Note: TestAdapter does not use testClassLoader
  final override protected def testClassPath: Array[File] = null

  final override protected def sourceMapper: Option[SourceMapper] = createAfterLink
    .mainModule
    .sourceMapName
    .map((name: String) => Files.file(directory = linkTask.getJSDirectory, segments = name))
    .map(ClosureCompilerSourceMapper(_))

  final override protected def testEnvironment: TestEnvironment =
    val afterLink: AfterLink = createAfterLink

    val testAdapter: TestAdapter = TestAdapter(
      jsEnv = afterLink.jsEnv,
      input = Seq(afterLink.input),
      config = TestAdapter.Config().withLogger(afterLink.jsLogger)
    )

    new TestEnvironment:
      override def loadFrameworks(descriptors: List[FrameworkDescriptor]): List[Framework] = testAdapter
        .loadFrameworks(descriptors.map(descriptor => List(descriptor.implementationClassName)))
        .flatten

      override def close(): Unit =
        testAdapter.close()
