package org.podval.tools.scalajsplugin.jvm

import org.podval.tools.build.GradleClassPath
import org.podval.tools.test.environment.{SourceMapper, TestEnvironment}
import org.podval.tools.test.framework.FrameworkDescriptor
import org.slf4j.{Logger, LoggerFactory}
import sbt.testing.Framework
import java.io.File

class JvmTestEnvironment extends TestEnvironment:
  override def close(): Unit = ()

  final override def sourceMapper: Option[SourceMapper] = None

  override protected def frameworksToLoad: List[FrameworkDescriptor] = FrameworkDescriptor.jvmSupported

  override protected def loadFrameworks(
    testClassPath: Iterable[File],
    frameworksToLoad: List[FrameworkDescriptor]
  ): List[Framework] =
    // This is the only way I know to:
    // - instantiate test frameworks from a classloader that has them and
    // - return sbt.testing.Framework used elsewhere, instead of something loaded from a different classloader
    //  (and thus can not be cast)
    GradleClassPath.addTo(this, testClassPath)

    def maybeInstantiate(descriptor: FrameworkDescriptor): Option[Framework] =
      try descriptor.newInstance match
        case framework: Framework => Some(framework)
        case other =>
          JvmTestEnvironment.logger.error(s"${other.getClass.getName} is not an SBT framework!")
          None
      catch
        case _: ClassNotFoundException => None
    
    frameworksToLoad.flatMap(maybeInstantiate)

object JvmTestEnvironment:
  private val logger: Logger = LoggerFactory.getLogger(JvmTestEnvironment.getClass)
  