package org.podval.tools.test

import org.opentorah.build.Gradle
import sbt.testing.Framework
import java.io.File
import scala.jdk.CollectionConverters.*

abstract class TestTaskScala extends TestTask:
  setDescription(s"Test using sbt frameworks")

  final override protected def testClassPath: Array[File] = getClasspath.asScala.toArray

  final override protected def sourceMapper: Option[SourceMapper] = None

  final override protected def testEnvironment: TestEnvironment =
    new TestEnvironment:
      override def close(): Unit = ()

      override def loadFrameworks(descriptors: List[FrameworkDescriptor]): List[Framework] =
        Gradle.addToClassPath(this, testClassPath)

        for
          descriptor <- descriptors
          framework <-
            try Class.forName(descriptor.implementationClassName).getConstructor().newInstance() match
              case framework: Framework => Some(framework)
              case other =>
                getLogger.error(s"--- ${other.getClass.getName} is not an SBT framework")
                None
            catch
              case _: ClassNotFoundException => None
        yield
          framework
