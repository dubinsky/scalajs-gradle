package org.podval.tools.build

import org.podval.tools.util.Strings.prefix

final class DependencyWithVersion(
  dependency: Dependency,
  val version: PreVersion
):
  def artifactName: String = s"${dependency.maker.artifact}${dependency.artifactNameSuffix}"
  
  def classifier: Option[String] = dependency.maker.classifier(version)
  
  def extension: Option[String] = dependency.maker.extension(version)

  def dependencyNotation: String =
    s"${dependency.maker.group}:$artifactName:$version${prefix(":", classifier)}${prefix("@", extension)}"

  def fileName: String =
    s"$artifactName-$version${prefix("-", classifier)}.${extension.getOrElse("jar")}"
