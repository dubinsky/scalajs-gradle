package org.podval.tools.build

import org.slf4j.{Logger, LoggerFactory}

abstract class Dependency(
  final override val group: String,
  final override val artifact: String
) extends DependencyCoordinates:
  
  final def withVersion(version: Version): Dependency.WithVersion =
    Dependency.WithVersion(dependency = this, version)

  final def artifactName: String = s"$artifact$artifactNameSuffix"

  protected def artifactNameSuffix: String

  final def verifyRequired(
    found: Dependency.WithVersion,
    version: Version,
    isVersionExact: Boolean
  ): Unit =
    if isVersionExact && found.version != version then
      Dependency.logger.warn(s"Found $found, but the project uses version $version")
    verifyRequiredMore()

  protected def verifyRequiredMore(): Unit = ()

object Dependency:
  private val logger: Logger = LoggerFactory.getLogger(Dependency.getClass)
  
  open class WithVersion(
    dependency: Dependency,
    version: Version
  ) extends DependencyData(
    group = Some(dependency.group),
    artifactName = dependency.artifactName,
    version = version,
    classifier = dependency.classifier(version),
    extension = dependency.extension(version)
  )

  trait Maker[-P]:
    def group: String
    def artifact: String
    def versionDefault: Version
    
    def findable(platform: P): FindableDependency[?]
    
    def dependency(platform: P): Dependency

    final def dependencyWithVersion(platform: P, version: Version): WithVersion =
      dependency(platform).withVersion(version)

    final def required(
      platform: P,
      version: Version = versionDefault,
      reason: String,
      configurationName: String,
      isVersionExact: Boolean = false
    ): DependencyRequirement = DependencyRequirement(
      findable = findable(platform),
      dependency = dependency(platform),
      version,
      reason,
      configurationName,
      isVersionExact
    )
    