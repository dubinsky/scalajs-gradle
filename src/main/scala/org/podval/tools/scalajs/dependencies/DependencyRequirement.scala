package org.podval.tools.scalajs.dependencies

import org.gradle.api.artifacts.{Configuration, Dependency as GDependency}
import org.gradle.api.{GradleException, Project}
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.opentorah.build.Gradle.*

final class DependencyRequirement(
  dependency: Dependency,
  version: String,
  scala2versionMinor: String,
  reason: String,
  isTest: Boolean = false,
  isVersionExact: Boolean = false,
  configurationName: Option[String] = None
):
  def applyToConfiguration(project: Project): DependencyVersion =
    val configuration: Configuration = getConfiguration(project)
    val found: Option[DependencyVersion] = dependency.getFromConfiguration(configuration)
    found.foreach(verify(_, project))
    found.getOrElse {
      val toAdd: DependencyVersion = dependencyVersion
      val gDependency: GDependency = project.getDependencies.create(toAdd.dependencyNotation)
      project.getLogger.info(s"Adding dependency $toAdd to the $configuration $reason", null, null, null)
      configuration
        .getDependencies
        .add(gDependency)
      toAdd
    }

  // Note: Once a classpath is resolved (e.g., by enumerating JARs on it :)),
  // dependencies can not be added to the configurations involved:
  //   Cannot change dependencies of dependency configuration ... after it has been included in dependency resolution
  // So the only thing we can do is to report the missing dependency:
  def applyToClasspath(project: Project): DependencyVersion =
    val sourceSet: SourceSet = project.getSourceSet(getSourceSetName)
    // Note: getConfiguration() can not be used as a java.lang.Iterable[File],
    // since it is not resolvable...
    val found: Option[DependencyVersion] = dependency.getFromClasspath(sourceSet.getRuntimeClasspath)
    found.foreach(verify(_, project))
    found.getOrElse {
      val configuration: Configuration = getConfiguration(project)
      val toAdd: DependencyVersion = dependencyVersion
      throw GradleException(
        s"Please add dependency $toAdd to the $configuration $reason"
      )
    }

  private def verify(found: DependencyVersion, project: Project): Unit =
    found match
      case s2: Scala2DependencyVersion =>
        if s2.scala2versionMinor != scala2versionMinor then project.getLogger.info(
          s"Found $s2, but the project uses Scala 2 version $scala2versionMinor", null, null, null
        )
      case _ =>

    if isVersionExact && found.version != version then project.getLogger.info(
      s"Found $found, but the project uses version $version", null, null, null
    )

  private def getConfiguration(project: Project): Configuration =
    GradleUtil.getConfiguration(project, getConfigurationName)

  private def getConfigurationName: String = configurationName.getOrElse(
    if isTest
    then JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
    else JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
  )

  private def getSourceSetName: String =
    if isTest
      then SourceSet.TEST_SOURCE_SET_NAME
      else SourceSet.MAIN_SOURCE_SET_NAME

  private def dependencyVersion: DependencyVersion = dependency match
    case s2: Scala2Dependency => s2.apply(version, scala2versionMinor)
    case s : SimpleDependency => s .apply(version                    )
