package org.podval.tools.testing.task

import org.opentorah.build.Configurations
import org.podval.tools.testing.framework.FrameworkDescriptor
import sbt.testing.Framework

abstract class TestTaskScala extends TestTask:
  setDescription(s"Test using sbt frameworks")
  final override protected def canFork: Boolean = true
  final override def sourceMapper: Option[SourceMapper] = None

  final override def testEnvironment: TestEnvironment = new TestEnvironment:
    override def close(): Unit = ()

    override def loadFrameworks(descriptors: List[FrameworkDescriptor]): List[Framework] =
      Util.addConfigurationToClassPath(TestTaskScala.this, Configurations.testImplementation.classPath)

      def maybeInstantiate(descriptor: FrameworkDescriptor): Option[Framework] =
        val result: Option[Framework] =
          try descriptor.newInstance match
            case framework: Framework => Some(framework)
            case other =>
              getLogger.error(s"${other.getClass.getName} is not an SBT framework!")
              None
          catch
            case _: ClassNotFoundException => None

        val message: String = if result.isEmpty then "not detected" else "detected"
        getLogger.info(s"Test framework ${descriptor.name} $message", null, null, null)

        result

      descriptors.flatMap(maybeInstantiate)


