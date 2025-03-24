package org.podval.tools.test.environment

import org.podval.tools.build.GradleClassPath
import org.podval.tools.test.framework.FrameworkDescriptor
import org.slf4j.{Logger, LoggerFactory}
import sbt.testing.Framework
import java.io.File

// Based on org.scalajs.testing.adapter.TestAdapter.
abstract class TestEnvironment:
  final def loadedFrameworks(testClassPath: Iterable[File]): List[Framework] =
    // This is the only way I know to:
    // - instantiate test frameworks from a classloader that has them and
    // - return sbt.testing.Framework used elsewhere, instead of something loaded from a different classloader
    //  (and thus can not be cast)
    if expandClassPath then GradleClassPath.addTo(this, testClassPath)

    val result: List[Framework] = loadFrameworks(frameworksToLoad)
    
    val report: String = result.map(framework => FrameworkDescriptor.forName(framework.name).displayName).mkString(", ")
    TestEnvironment.logger.info(s"Loaded test frameworks: $report")

    // Check uniqueness; implementation class can not be used since in Scala.js mode they all are
    // `org.scalajs.testing.adapter.FrameworkAdapter`.
    require(result.map(_.name).toSet.size == result.size, "Different frameworks with the same name!")
    
    result

  protected def expandClassPath: Boolean
  
  protected def frameworksToLoad: List[FrameworkDescriptor]

  protected def loadFrameworks(frameworksToLoad: List[FrameworkDescriptor]): List[Framework]
  
  def sourceMapper: Option[SourceMapper]
  
  def close(): Unit

object TestEnvironment:
  private val logger: Logger = LoggerFactory.getLogger(TestEnvironment.getClass)
  