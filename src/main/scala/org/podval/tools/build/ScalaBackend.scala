package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.scalajs.ScalaJSBackend
import org.podval.tools.scalanative.ScalaNativeBackend
import org.podval.tools.test.framework.FrameworkDescriptor
import org.slf4j.{Logger, LoggerFactory}
import sbt.testing.Framework
import java.io.File

object ScalaBackend:
  final class DependencyRequirements(
    val implementation: Array[DependencyRequirement],
    val testRuntimeOnly: Array[DependencyRequirement],
    val scalaCompilerPlugins: Array[DependencyRequirement],
    val testScalaCompilerPlugins: Array[DependencyRequirement],
    val pluginDependencies: Array[DependencyRequirement]
  )

  val sharedSourceRoot: String = "shared"
  def all: Set[ScalaBackend] = Set(JvmBackend, ScalaJSBackend, ScalaNativeBackend)
  def names: String = all.map(backend => s"${backend.name} (${backend.sourceRoot})").mkString(", ")
  def sourceRoots: String = all.map(_.sourceRoot).mkString(", ")

abstract class ScalaBackend(
  val name: String,
  val sourceRoot: String,
  val artifactSuffix: Option[String],
  val archiveAppendix: Option[String],
  val testsCanNotBeForked: Boolean,
  val pluginDependenciesConfigurationName: Option[String],
  val createExtension: Option[CreateExtension[?]]
) derives CanEqual:
  final def is(name: String): Boolean =
    name.toLowerCase == this.name      .toLowerCase ||
    name.toLowerCase == this.sourceRoot.toLowerCase

  final def artifactSuffixString: String = artifactSuffix.map(suffix => s"_$suffix").getOrElse("")

  final def describe(what: String): String = s"$name $what."

  def scalaCompileParameters(scalaVersion: ScalaVersion): Seq[String]

  def dependencyRequirements(
    implementationConfiguration: Configuration,
    testImplementationConfiguration: Configuration,
    scalaVersion: ScalaVersion
  ): ScalaBackend.DependencyRequirements

  // TODO if task classes are turned into parameters, I get NoClassDefFoundError: org/gradle/api/Task...
  // Do I need to add gradle-api jar in SbtTestFramework?
  def linkTaskClassOpt    : Option[Class[? <: LinkTask.Main]]
  def testLinkTaskClassOpt: Option[Class[? <: LinkTask.Test]]
  def runTaskClassOpt     : Option[Class[? <: RunTask .Main]]
  def testTaskClass       :        Class[? <: RunTask .Test]

  // Based on org.scalajs.testing.adapter.TestAdapter.
  trait TestEnvironment:
    final def backend: ScalaBackend = ScalaBackend.this
    
    private val logger: Logger = LoggerFactory.getLogger(getClass)
    
    def sourceMapper: Option[SourceMapper]

    protected def expandClassPath: Boolean

    protected def loadFrameworks: List[Framework]

    def close(): Unit

    final protected def frameworksToLoad: List[FrameworkDescriptor] = FrameworkDescriptor.forBackend(backend)
    
    final def loadedFrameworks(testClassPath: Iterable[File]): List[Framework] =
      // This is the only way I know to:
      // - instantiate test frameworks from a classloader that has them and
      // - return sbt.testing.Framework used elsewhere, instead of something loaded from a different classloader
      //  (and thus can not be cast)
      if expandClassPath then GradleClassPath.addTo(this, testClassPath)

      val result: List[Framework] = loadFrameworks

      logger.info(
        s"Loaded test frameworks: ${result.map(framework => FrameworkDescriptor.forName(framework.name).displayName).mkString(", ")}"
      )

      // Check uniqueness; implementation class cannot be used since in Scala.js mode they all are
      // `org.scalajs.testing.adapter.FrameworkAdapter`.
      require(result.map(_.name).toSet.size == result.size, "Different frameworks with the same name!")

      if result.isEmpty then logger.warn(s"No test frameworks on the classpath: ${testClassPath.mkString(", ")}.")

      result
