package org.podval.tools.scalajsplugin.jvm

import org.podval.tools.test.environment.{SourceMapper, TestEnvironment}
import org.podval.tools.test.framework.FrameworkDescriptor
import org.slf4j.{Logger, LoggerFactory}
import sbt.testing.Framework
import java.io.File

final class JvmTestEnvironment extends TestEnvironment:
  override def close(): Unit = ()

  override def sourceMapper: Option[SourceMapper] = None

  protected def expandClassPath: Boolean = true

  override protected def frameworksToLoad: List[FrameworkDescriptor] = FrameworkDescriptor.jvmSupported

  override protected def loadFrameworks(frameworksToLoad: List[FrameworkDescriptor]): List[Framework] =
    frameworksToLoad.flatMap(maybeInstantiate)

  private def maybeInstantiate(descriptor: FrameworkDescriptor): Option[Framework] =
    try descriptor.newInstance match
      case framework: Framework => Some(framework)
      case other =>
        JvmTestEnvironment.logger.error(s"${other.getClass.getName} is not an SBT framework!")
        None
    catch
      case _: ClassNotFoundException => None
      
object JvmTestEnvironment:
  private val logger: Logger = LoggerFactory.getLogger(JvmTestEnvironment.getClass)
  