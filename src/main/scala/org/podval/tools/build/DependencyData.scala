package org.podval.tools.build

import org.podval.tools.util.{Files, Strings}
import java.io.File

class DependencyData(
  val group: Option[String],
  val artifactName: String,
  val version: Version,
  val classifier: Option[String],
  val extension: Option[String]
):
  final override def toString: String = s"'$dependencyNotation'"
  
  final def groupMatches(group: String): Boolean =
    this.group.isEmpty || this.group.contains(group)

  final def classifierMatches(classifier: Option[String]): Boolean =
    classifier == this.classifier

  final def extensionMatches(extension: Option[String]): Boolean =
    (extension == this.extension) ||
    (extension.isEmpty && this.extension.contains("jar"))
      
  final def dependencyNotation: String =
    val classifierStr: String = Strings.prefix(":", classifier)
    val extensionStr: String = Strings.prefix("@", extension)
    s"${group.get}:$artifactName:$version$classifierStr$extensionStr"

  final def fileName: String =
    val classifierStr: String = Strings.prefix("-", classifier)
    val extensionStr: String = extension.getOrElse("jar")
    s"$artifactName-$version$classifierStr.$extensionStr"

object DependencyData:
  def fromGradleDependency(
    dependency: org.gradle.api.artifacts.Dependency,
    isVersionCompound: Boolean
  ): Option[DependencyData] = dependency match
    case dependency: org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency =>
      Option(dependency.getVersion).flatMap(Version(_, isVersionCompound).map(version =>
        DependencyData(
          group = Some(dependency.getGroup),
          artifactName= dependency.getName,
          version = version,
          classifier = None,
          extension = Some("jar")
        )
      ))
    case _ => None

  def fromFile(
    file: File,
    isVersionCompound: Boolean
  ): Option[DependencyData] =
    val (nameAndVersion: String, fileExtension: Option[String]) = Files.nameAndExtension(file.getName)
    val (name: String, versionOpt: Option[String]) = Strings.split(nameAndVersion, '-')
    versionOpt.flatMap(Version(_, isVersionCompound).map(version =>
      DependencyData(
        group = None,
        artifactName = name,
        version = version,
        classifier = None,
        extension = fileExtension
      )
    ))
