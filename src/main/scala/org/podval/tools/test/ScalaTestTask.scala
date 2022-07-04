package org.podval.tools.test

import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.opentorah.build.Gradle.addConfigurationToClassPath
import sbt.testing.Framework
import java.net.URLClassLoader
import scala.jdk.CollectionConverters.*

class ScalaTestTask extends TestTask:
  setDescription(s"Test using sbt frameworks")

  final override protected def testEnvironment: TestEnvironment =
    addConfigurationToClassPath(this, ScalaBasePlugin.ZINC_CONFIGURATION_NAME)

    new TestEnvironment(
      sourceMapper = None,
      testClassLoader = URLClassLoader(
        getClasspath.asScala.map(_.toURI.toURL).toArray,
        getClass.getClassLoader
      ),
    ):
      override def close(): Unit = ()

      override def loadFrameworks(descriptors: List[TestEnvironment.FrameworkDescriptor]): List[Framework] =
        for
          descriptor <- descriptors
          className <- descriptor.implClassNames
          framework <-
            try Class.forName(className, false, testClassLoader).getConstructor().newInstance() match
              case framework: Framework => Some(framework)
              case other =>
                println(s"--- ${other.getClass.getName} is not an SBT framework")
                None
            catch
              case _: ClassNotFoundException => None
        yield
          framework
