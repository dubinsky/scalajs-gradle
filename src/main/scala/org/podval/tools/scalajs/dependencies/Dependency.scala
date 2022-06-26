package org.podval.tools.scalajs.dependencies

import org.gradle.api.artifacts.{Dependency as GDependency}
import org.gradle.api.Project
import org.opentorah.util.Strings
import java.io.File
import java.util.regex.{Matcher, Pattern}
import GradleUtil.*
import scala.jdk.CollectionConverters.*

// TODO merge into org.opentorah.build
abstract class DependencyVersion(
  val dependency: Dependency,
  val version: String
):
  final override def toString: String = s"'$dependencyNotation'"

  final def dependencyNotation: String = s"${dependency.group}:$nameDependencyNotation:$version"

  def nameDependencyNotation: String

object DependencyVersion:
  def getMajor(version: String): String = Strings.splitRight(version, '.')._1.get

abstract class Dependency(
  val group: String,
  val nameBase: String
):
  protected def namePattern: String

  def getFromConfiguration(configurationNames: ConfigurationNames, project: Project): Option[DependencyVersion] =
    val patternCompiled: Pattern = Pattern.compile(namePattern)
    val result: Set[DependencyVersion] = for
      dependency: GDependency <- project.getConfiguration(configurationNames.toAdd).getDependencies.asScala.toSet
      if dependency.getGroup == group
      matcher: Matcher = patternCompiled.matcher(dependency.getName)
      if matcher.matches
    yield
      fromMatcher(matcher, dependency.getVersion)
    result.headOption

  protected def fromMatcher(matcher: Matcher, version: String): DependencyVersion

  def getFromClasspath(configurationNames: ConfigurationNames, project: Project): Option[DependencyVersion] =
    val patternCompiled: Pattern = Pattern.compile(s"$namePattern-(\\d.*).jar")
    val result: Iterable[DependencyVersion] = for
      file: File <- project.getConfiguration(configurationNames.toCheck).asScala
      matcher: Matcher = patternCompiled.matcher(file.getName)
      if matcher.matches
    yield
      fromMatcher(matcher)
    result.headOption

  protected def fromMatcher(matcher: Matcher): DependencyVersion
