package org.podval.tools.build

import org.podval.tools.test.framework.FrameworkDescriptor
import org.slf4j.{Logger, LoggerFactory}
import sbt.testing.Framework
import java.io.File

// Based on org.scalajs.testing.adapter.TestAdapter.
object TestEnvironment:
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  trait Creator[B <: ScalaBackend]:
    def testEnvironment: TestEnvironment[B]

abstract class TestEnvironment[B <: ScalaBackend](
  val backend: B,
  val sourceMapper: Option[SourceMapper]
):
  protected def loadFrameworks: List[Framework]

  def close(): Unit

  final protected def frameworksToLoad: List[FrameworkDescriptor] = FrameworkDescriptor.forBackend(backend)

  final def loadedFrameworks(testClassPath: Iterable[File]): List[Framework] =
    // This is the only way I know to:
    // - instantiate test frameworks from a classloader that has them and
    // - return sbt.testing.Framework used elsewhere, instead of something loaded from a different classloader
    //  (and thus can not be cast)
    if backend.expandClassPathForTestEnvironment then GradleClassPath.addTo(this, testClassPath)

    val result: List[Framework] = loadFrameworks

    TestEnvironment.logger.info(
      s"Loaded test frameworks for $backend: ${result.map(framework => FrameworkDescriptor.forName(framework.name).displayName).mkString(", ")}"
    )

    // Check uniqueness; implementation class cannot be used since in Scala.js mode they all are
    // `org.scalajs.testing.adapter.FrameworkAdapter`.
    require(result.map(_.name).toSet.size == result.size, "Different frameworks with the same name!")

    if result.isEmpty then TestEnvironment.logger.warn(s"No test frameworks for $backend on the classpath: ${testClassPath.mkString(", ")}.")

    result
