package org.podval.tools.test.environment

import org.podval.tools.test.framework.FrameworkDescriptor
import org.slf4j.{Logger, LoggerFactory}
import sbt.testing.Framework
import java.io.File

// Based on org.scalajs.testing.adapter.TestAdapter.
abstract class TestEnvironment:
  final def loadedFrameworks(testClassPath: Iterable[File]): List[Framework] =
    val result: List[Framework] = loadFrameworks(testClassPath, frameworksToLoad)
    
    val report: String = result.map(framework => FrameworkDescriptor.forName(framework.name).displayName).mkString(", ")
    TestEnvironment.logger.info(s"Loaded test frameworks: $report")

    // Check uniqueness; implementation class can not be used since in Scala.js mode they all are
    // `org.scalajs.testing.adapter.FrameworkAdapter`.
    require(result.map(_.name).toSet.size == result.size, "Different frameworks with the same name!")
    
    result

  protected def frameworksToLoad: List[FrameworkDescriptor]

  protected def loadFrameworks(
    testClassPath: Iterable[File],
    frameworksToLoad: List[FrameworkDescriptor]
  ): List[Framework]
  
  def sourceMapper: Option[SourceMapper]
  
  def close(): Unit

object TestEnvironment:
  private val logger: Logger = LoggerFactory.getLogger(TestEnvironment.getClass)
  