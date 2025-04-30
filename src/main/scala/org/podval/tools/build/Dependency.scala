package org.podval.tools.build

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
  
  trait Maker[-P]:
    def group: String
    def artifact: String
    def versionDefault: Version
    def description: String
    def useExactVersionInVerifyRequired: Boolean = false
    def findable(platform: P): FindableDependency[?]
    def dependency(platform: P): Dependency

    final def required(version: Version = versionDefault): DependencyRequirement[P] = DependencyRequirement(
      this,
      version
    )
    