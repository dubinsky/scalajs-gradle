package org.podval.tools.build

import org.podval.tools.util.{Files, Strings}
import java.io.File

trait DependencyData:
  def group: Option[String]
  def artifactName: String
  def classifier: Option[String]
  def extension: Option[String]
  def version: Version

  final def dependencyNotation: String =
    val classifierStr: String = Strings.prefix(":", classifier)
    val extensionStr: String = Strings.prefix("@", extension)
    s"${group.get}:$artifactName:${version.version}$classifierStr$extensionStr"

  final def fileName: String =
    val classifierStr: String = Strings.prefix("-", classifier)
    val extensionStr: String = extension.getOrElse("jar")
    s"$artifactName-${version.version}$classifierStr.$extensionStr"

object DependencyData:
  def fromGradleDependency(dependency: org.gradle.api.artifacts.Dependency): Option[DependencyData] = dependency match
    case dependency: org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency =>
      Some(new DependencyData:
        override def group: Option[String] = Some(dependency.getGroup)
        override def artifactName: String = dependency.getName
        override def version: Version = Version(dependency.getVersion)
        override def classifier: Option[String] = None
        override def extension: Option[String] = Some("jar")
      )
    case _ => None

  def fromFile(file: File): Option[DependencyData] =
    val (nameAndVersion: String, fileExtension: Option[String]) = Files.nameAndExtension(file.getName)
    val (name: String, versionOpt: Option[String]) = Strings.split(nameAndVersion, '-')
    if versionOpt.isEmpty then None else
      Some(new DependencyData:
        override def group: Option[String] = None
        override def artifactName: String = name
        override def version: Version = Version(versionOpt.get)
        override def classifier: Option[String] = None
        override def extension: Option[String] = fileExtension
      )
