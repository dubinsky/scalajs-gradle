package org.podval.tools.scalajs.dependencies

import java.util.regex.Matcher

final class Scala2DependencyVersion(
  override val dependency: Scala2Dependency,
  version: String,
  val scalaVersion: String
) extends DependencyVersion(
  dependency = dependency,
  version = version
):
  final override def nameDependencyNotation: String = s"${dependency.nameBase}_$scalaVersion"

open class Scala2Dependency(
  group: String,
  nameBase: String,
  val isScala2versionFull: Boolean = false
) extends Dependency(
  group = group,
  nameBase = nameBase
):
  final override protected def namePattern: String = s"${nameBase}_(\\d.*)"

  final override protected def fromMatcher(matcher: Matcher, version: String): Scala2DependencyVersion = apply(
    scalaVersion = matcher.group(1),
    version = version
  )

  final override protected def fromMatcher(matcher: Matcher): Scala2DependencyVersion = apply(
    scalaVersion = matcher.group(1),
    version = matcher.group(2)
  )

  def apply(
    scalaVersion: String,
    version: String
  ): Scala2DependencyVersion = Scala2DependencyVersion(
    dependency = this,
    scalaVersion = scalaVersion,
    version = version
  )
