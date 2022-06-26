package org.podval.tools.scalajs.dependencies

import org.gradle.api.artifacts.Configuration
import org.gradle.api.{GradleException, Project}
import org.opentorah.build.Gradle.*
import GradleUtil.*

final class DependencyRequirement(
  dependency: Dependency,
  version: String,
  scalaLibrary: ScalaLibrary,
  reason: String,
  isVersionExact: Boolean = false,
  configurationNames: ConfigurationNames = ConfigurationNames.implementation
):
  def applyToConfiguration(project: Project): DependencyVersion =
    val found: Option[DependencyVersion] = dependency.getFromConfiguration(configurationNames, project)
    found.foreach(verify(_, project))
    found.getOrElse {
      val (configuration: Configuration, toAdd: DependencyVersion) = addition(project)
      project.getLogger.info(s"Adding dependency $toAdd to the $configuration $reason", null, null, null)
      configuration
        .getDependencies
        .add(project.getDependencies.create(toAdd.dependencyNotation))
      toAdd
    }

  // Note: Once a classpath is resolved (e.g., by enumerating JARs on it :)),
  // dependencies can not be added to the configurations involved:
  //   Cannot change dependencies of dependency configuration ... after it has been included in dependency resolution
  // So the only thing we can do is to report the missing dependency:
  def applyToClasspath(project: Project): DependencyVersion =
    val found: Option[DependencyVersion] = dependency.getFromClasspath(configurationNames, project)
    found.foreach(verify(_, project))
    found.getOrElse {
      val (configuration: Configuration, toAdd: DependencyVersion) = addition(project)
      throw GradleException(s"Please add dependency $toAdd to the $configuration $reason")
    }

  private def verify(found: DependencyVersion, project: Project): Unit =
    found match
      case s2: Scala2DependencyVersion =>
        val scalaVersion: String = getScalaVersion(s2.dependency)
        if s2.scalaVersion != scalaVersion then project.getLogger.info(
          s"Found $s2, but the project uses Scala 2 version $scalaVersion", null, null, null
        )
      case _ =>

    if isVersionExact && found.version != version then project.getLogger.info(
      s"Found $found, but the project uses version $version", null, null, null
    )

  private def addition(project: Project): (Configuration, DependencyVersion) =
    val configuration: Configuration = project.getConfiguration(configurationNames.toAdd)
    val dependencyVersion: DependencyVersion = dependency match
    case s2: Scala2Dependency => s2.apply(
      scalaVersion = getScalaVersion(s2),
      version = version
    )
    case s : SimpleDependency => s .apply(
      version = version
    )
    (configuration, dependencyVersion)

  private def getScalaVersion(s2: Scala2Dependency): String =
    if s2.isScala2versionFull
    then scalaLibrary.scala2
      .get
      .version
    else scalaLibrary.scala2
      .map(scala2 => DependencyVersion.getMajor(scala2.version))
      .getOrElse(ScalaLibrary.scala2versionMinor(scalaLibrary.scala3.get.version))
