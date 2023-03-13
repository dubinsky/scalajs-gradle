package org.podval.tools.testing.task

import org.podval.tools.testing.framework.FrameworkDescriptor
import org.opentorah.build.Gradle.*
import sbt.testing.Framework
import java.io.File

abstract class TestTaskScala extends TestTask:
  setDescription(s"Test using sbt frameworks")
  final override protected def canFork: Boolean = true

  final override protected def sourceMapper: Option[SourceMapper] = None

  final override protected def testEnvironment: TestEnvironment = new TestEnvironment:
    override def close(): Unit = ()

    override def loadFrameworks(testClassPath: Iterable[File]): List[Framework] =
      // Note: this is the only way I know to:
      // - instantiate test frameworks from a classloader that has them and
      // - return sbt.testing.Framework used elsewhere, instead of something loaded from a different classloader
      //  (and thus can not be cast)
      addToClassPath(TestTaskScala.this, testClassPath)

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

      FrameworkDescriptor.scalaSupported.flatMap(maybeInstantiate)
