package org.podval.tools.test.task

import org.podval.tools.build.Backend
import org.podval.tools.gradle.GradleClasspath
import org.podval.tools.test.framework.Framework
import org.slf4j.{Logger, LoggerFactory}
import java.io.File

// Based on org.scalajs.testing.adapter.TestAdapter.
object TestEnvironment:
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  trait Creator[B <: Backend]:
    def testEnvironment: TestEnvironment[B]

abstract class TestEnvironment[B <: Backend](
  val backend: B,
  val sourceMapper: Option[SourceMapper]
):
  protected def loadFrameworks: List[Framework.Loaded]

  def close(): Unit

  final def frameworks: List[Framework] = Framework
    .all
    .toList
    .filter(_.isBackendSupported(backend))

  final def loadFrameworks(testClasspath: Iterable[File]): List[Framework.Loaded] =
    // This is the only way I know to:
    // - instantiate test frameworks from a classloader that has them and
    // - return sbt.testing.Framework used elsewhere, instead of something loaded from a different classloader
    //  (and thus can not be cast)
    if backend.expandClasspathForTestEnvironment then GradleClasspath.addTo(filesToAdd = testClasspath)

    val result: List[Framework.Loaded] = loadFrameworks

    TestEnvironment.logger.info(
      s"Loaded test frameworks for $backend: ${result.map(_.framework.name).mkString(", ")}."
    )

    // Check uniqueness; implementation class cannot be used since in Scala.js mode they all are
    // `org.scalajs.testing.adapter.FrameworkAdapter`.
    require(result.map(_.nameSbt).toSet.size == result.size, "Different frameworks with the same name!")

    if result.isEmpty then TestEnvironment.logger.warn(s"No test frameworks for $backend on the classpath: ${testClasspath.mkString(", ")}.")

    result
