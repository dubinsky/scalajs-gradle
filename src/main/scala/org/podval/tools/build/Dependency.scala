package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.podval.tools.backend.ScalaBackend

abstract class Dependency(
  final override val group: String,
  final override val artifact: String
) extends DependencyCoordinates:
  
  final def withVersion(version: Version): Dependency.WithVersion =
    Dependency.WithVersion(dependency = this, version)

  final def artifactName: String = s"$artifact$artifactNameSuffix"

  protected def artifactNameSuffix: String

object Dependency:
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
  
  trait Maker:
    override def toString: String = s"$group:$artifact"

    def scalaBackend: ScalaBackend
    def group: String
    def artifact: String
    def versionDefault: Version.Simple
    def description: String
    def useExactVersionInVerifyRequired: Boolean = false
    def findable: FindableDependency[?]
    def dependency(scalaVersion: ScalaVersion): Dependency

    final def findInConfiguration(configuration: Configuration): Option[Dependency.WithVersion] = findable
      .findInConfiguration(configuration)

    final def required(version: Version = versionDefault): DependencyRequirement = DependencyRequirement(
      this,
      version
    )
