package org.podval.tools.build

import org.gradle.api.artifacts.{Configuration, Dependency as DependencyG}
import org.podval.tools.util.Strings
import scala.jdk.CollectionConverters.SetHasAsScala
import java.io.File

trait DependencyFindable[D <: Dependency]:
  def maker: DependencyMaker

  protected def dependencyForArtifactName(artifactName: String): Option[D]

  import DependencyFindable.DependencyData

  private def forVersion(version: Option[String], mk: PreVersion => DependencyData): Option[DependencyData] = 
    version.flatMap(PreVersion(_, maker.isVersionCompound)).map(mk)
  
  final def findInConfiguration(configuration: Configuration): Option[Dependency#WithVersion] = find(configuration
    .getDependencies
    .asScala
    .flatMap((dependency: DependencyG) => 
      forVersion(Option(dependency.getVersion), DependencyData(_)(
        group = Option(dependency.getGroup),
        artifactName = dependency.getName,
        classifier = None,
        extension = Some("jar")
      ))
    )
  )

  final def findInClasspath(classpath: Iterable[File]): Option[Dependency#WithVersion] = find(classpath
    .flatMap((file: File) =>
      val (nameAndVersion: String, extension: Option[String]) = Strings.split(file.getName  , '.')
      val (name          : String, version  : Option[String]) = Strings.split(nameAndVersion, '-')
      forVersion(version, DependencyData(_)(
        group = None,
        artifactName = name,
        classifier = None,
        extension = extension
      ))
    )
  )

  private def find(iterable: Iterable[DependencyFindable.DependencyData]): Option[Dependency#WithVersion] =
    iterable.flatMap(find).headOption

  private def find(dependencyData: DependencyFindable.DependencyData): Option[Dependency#WithVersion] =
    val version: PreVersion = dependencyData.version
    val extension: Option[String] = maker.extension(version)
    val matches: Boolean =       
      (dependencyData.group.isEmpty || dependencyData.group.contains(maker.group)) &&
      (dependencyData.classifier == maker.classifier(version)) &&
      ((dependencyData.extension == extension) || (extension.isEmpty && dependencyData.extension.contains("jar")))
    if !matches
    then None 
    else dependencyForArtifactName(dependencyData.artifactName).map(_.withVersion(version))

object DependencyFindable:
  private final class DependencyData(val version: PreVersion)(
    val group: Option[String],
    val artifactName: String,
    val classifier: Option[String],
    val extension: Option[String]
  )
