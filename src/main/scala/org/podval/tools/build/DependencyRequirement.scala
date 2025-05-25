package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.gradle.api.{GradleException, Project}
import org.slf4j.{Logger, LoggerFactory}

final class DependencyRequirement(
  maker: Dependency.Maker,
  version: Version
):
  def applyToConfiguration(
    project: Project,
    configuration: Configuration,
    scalaVersion: ScalaVersion
  ): Dependency.WithVersion =
    val findable: FindableDependency[?] = maker.findable
    val dependency: Dependency = maker.dependency(scalaVersion)

    val result: Dependency.WithVersion = findable.findInConfiguration(configuration).getOrElse:
      val toAdd: Dependency.WithVersion = dependency.withVersion(version)
      DependencyRequirement.logger.info(
        s"Adding dependency $toAdd to configuration '${project.getName}.${configuration.getName}': ${maker.description}"
      )
      configuration
        .getDependencies
        .add(project.getDependencies.create(toAdd.dependencyNotation))
      findable
        .findInConfiguration(configuration)
        .getOrElse(throw GradleException(s"Failed to add dependency $toAdd to configuration ${configuration.getName}."))
    
    if maker.useExactVersionInVerifyRequired && result.version != version then
      DependencyRequirement.logger.warn(s"Found $result, but the project uses version $version.")
      
    // TODO - implement? (using ScalaPlatform.artifactAndScalaVersion())
    //    override protected def verifyRequiredMore(): Unit = ()
    //    if dependency.isInstanceOf[Scala2Dependency] then
    //      val scalaVersion: String = getScalaVersion(dependency.asInstanceOf[Scala2Dependency])
    //      val version: Scala2Dependency.Version = result.version.asInstanceOf[Scala2Dependency.Version]
    //      if version.scalaVersion != scalaVersion then project.getLogger.info(
    //        s"Found $found, but the project uses Scala 2 version $scalaVersion", null, null, null
    //      )

    result

object DependencyRequirement:
  private val logger: Logger = LoggerFactory.getLogger(DependencyRequirement.getClass)
