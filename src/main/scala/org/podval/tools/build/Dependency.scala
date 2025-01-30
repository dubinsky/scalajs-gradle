package org.podval.tools.build

import org.gradle.api.Project

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
    isVersionExact: Boolean,
    project: Project
  ): Unit =
    if isVersionExact && found.version != version then project.getLogger.info(
      s"Found $found, but the project uses version $version", null, null, null
    )
    verifyRequiredMore()

  protected def verifyRequiredMore(): Unit = ()

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
