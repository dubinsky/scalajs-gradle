package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.podval.tools.util.{Files, Strings}
import scala.jdk.CollectionConverters.SetHasAsScala
import java.io.File

trait DependencyFindable[D <: Dependency]:
  def maker: DependencyMaker

  protected def dependencyForArtifactName(artifactName: String): Option[D]

  final def findInConfiguration(configuration: Configuration): Option[Dependency#WithVersion] = find(configuration
    .getDependencies
    .asScala
    .flatMap(DependencyFindable.fromGradleDependency(_, maker.isVersionCompound))
  )

  final def findInClassPath(classPath: Iterable[File]): Option[Dependency#WithVersion] = find(classPath
    .flatMap(DependencyFindable.fromFile(_, maker.isVersionCompound))
  )

  private def find(iterable: Iterable[DependencyFindable.DependencyData]): Option[Dependency#WithVersion] =
    iterable.flatMap(find).headOption

  private def find(dependencyData: DependencyFindable.DependencyData): Option[Dependency#WithVersion] =
    val version: PreVersion = dependencyData.version
    val extension = maker.extension(version)
    val matches: Boolean =       
      (dependencyData.group.isEmpty || dependencyData.group.contains(maker.group)) &&
      (dependencyData.classifier == maker.classifier(version)) &&
      ((dependencyData.extension == extension) || (extension.isEmpty && dependencyData.extension.contains("jar")))
    if !matches
    then None 
    else dependencyForArtifactName(dependencyData.artifactName).map(_.withVersion(version))

object DependencyFindable:
  private final class DependencyData(
    val group: Option[String],
    val artifactName: String,
    val version: PreVersion,
    val classifier: Option[String],
    val extension: Option[String]
  )

  private def fromGradleDependency(
    dependency: org.gradle.api.artifacts.Dependency,
    isVersionCompound: Boolean
  ): Option[DependencyData] = dependency match
    case dependency: org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency =>
      Option(dependency.getVersion).flatMap(PreVersion(_, isVersionCompound).map(version =>
        DependencyData(
          group = Some(dependency.getGroup),
          artifactName = dependency.getName,
          version = version,
          classifier = None,
          extension = Some("jar")
        )
      ))
    case _ => None

  private def fromFile(
    file: File,
    isVersionCompound: Boolean
  ): Option[DependencyData] =
    val (nameAndVersion: String, fileExtension: Option[String]) = Files.nameAndExtension(file.getName)
    val (name: String, versionOpt: Option[String]) = Strings.split(nameAndVersion, '-')
    versionOpt.flatMap(PreVersion(_, isVersionCompound).map(version =>
      DependencyData(
        group = None,
        artifactName = name,
        version = version,
        classifier = None,
        extension = fileExtension
      )
    ))
