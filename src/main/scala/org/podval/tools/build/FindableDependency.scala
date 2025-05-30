package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import scala.jdk.CollectionConverters.SetHasAsScala
import java.io.File

trait FindableDependency[D <: Dependency] extends DependencyCoordinates:
  def isVersionCompound: Boolean = false

  final def findInConfiguration(configuration: Configuration): Option[Dependency.WithVersion] = find(configuration
    .getDependencies
    .asScala
    .flatMap(DependencyData.fromGradleDependency(_, isVersionCompound))
  )

  final def findInClassPath(classPath: Iterable[File]): Option[Dependency.WithVersion] = find(classPath
    .flatMap(DependencyData.fromFile(_, isVersionCompound))
  )

  private def find(iterable: Iterable[DependencyData]): Option[Dependency.WithVersion] =
    iterable.flatMap(find).headOption

  private def find(dependencyData: DependencyData): Option[Dependency.WithVersion] =
    val version: Version = dependencyData.version
    val matches: Boolean =       
      dependencyData.groupMatches(group) &&
      dependencyData.classifierMatches(classifier(version)) &&
      dependencyData.extensionMatches(extension(version))
    if !matches
    then None 
    else dependencyForArtifactName(dependencyData.artifactName).map(_.withVersion(version))
  
  protected def dependencyForArtifactName(artifactName: String): Option[D]
