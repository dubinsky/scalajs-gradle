package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import scala.jdk.CollectionConverters.*
import java.io.File

trait Dependency extends DependencyCoordinates:

  def artifactName: String

  final def withVersion(version: Version): Dependency.WithVersion =
    Dependency.WithVersion(dependency = this, version)

object Dependency:

  trait Findable extends DependencyCoordinates:

    def dependencyForArtifactName(artifactName: String): Option[Dependency]

    final def findInConfiguration(configuration: Configuration): Option[Dependency.WithVersion] =
      find(configuration.getDependencies.asScala.flatMap(DependencyData.fromGradleDependency))

    final def findInClassPath(classPath: Iterable[File]): Option[Dependency.WithVersion] =
      find(classPath.flatMap(DependencyData.fromFile))

    private def find(iterable: Iterable[DependencyData]): Option[Dependency.WithVersion] =
      iterable.flatMap(find).headOption

    private def find(dependencyData: DependencyData): Option[Dependency.WithVersion] =
      val version: Version = dependencyData.version
      val groupMatches: Boolean = dependencyData.group.isEmpty || dependencyData.group.contains(group)
      val classifierMatches: Boolean = this.classifier(version) == dependencyData.classifier
      val extensionMatches: Boolean =
        (this.extension(version) == dependencyData.extension) ||
        (this.extension(version).isEmpty && dependencyData.extension.contains("jar"))

      if !groupMatches || !classifierMatches || !extensionMatches then None else
        dependencyForArtifactName(dependencyData.artifactName).map(_.withVersion(version))

  abstract class Simple(
    final override val group: String,
    final override val artifact: String
  ) extends Findable with Dependency:

    final override def dependencyForArtifactName(artifactName: String): Option[Dependency] =
      if artifactName == artifact then Some(this) else None

    final override def artifactName: String =
      artifact
      
  open class WithVersion(
    val dependency: Dependency,
    final override val version: Version
  ) extends DependencyData:
    final override def toString: String = s"'$dependencyNotation'"
    final override def group: Option[String] = Some(dependency.group)
    final override def artifactName: String = dependency.artifactName
    final override def classifier: Option[String] = dependency.classifier(version)
    final override def extension: Option[String] = dependency.extension(version)

