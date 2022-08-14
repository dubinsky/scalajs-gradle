package org.podval.tools.test

import org.opentorah.build.Gradle
import org.podval.tools.test.framework.FrameworkDescriptor
import sbt.testing.Framework
import scala.jdk.CollectionConverters.*

abstract class TestTaskScala extends TestTask:
  setDescription(s"Test using sbt frameworks")
  final override protected def canFork: Boolean = true
  final override protected def sourceMapper: Option[SourceMapper] = None

  final override protected def testEnvironment: TestEnvironment = new TestEnvironment:
    override def close(): Unit = ()

    override def loadFrameworks(descriptors: List[FrameworkDescriptor]): List[Framework] =
      Gradle.addToClassPath(this, getClasspath.asScala)
      descriptors.flatMap(_.maybeInstantiate)
