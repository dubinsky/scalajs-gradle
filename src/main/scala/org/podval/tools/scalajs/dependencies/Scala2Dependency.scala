package org.podval.tools.scalajs.dependencies

import java.util.regex.Matcher

final class Scala2DependencyVersion(
  dependency: Scala2Dependency,
  version: String,
  val scala2versionMinor: String
) extends DependencyVersion(
  dependency,
  version
):
  final override def nameDependencyNotation: String = s"${dependency.nameBase}_$scala2versionMinor"

open class Scala2Dependency(
  group: String,
  nameBase: String
) extends Dependency(
  group = group,
  nameBase = nameBase
):
  final override protected def namePattern: String = s"${nameBase}_(\\d.*)"

  final override protected def fromMatcher(matcher: Matcher, version: String): Scala2DependencyVersion = apply(
    version = version,
    scala2versionMinor = matcher.group(1)
  )

  final override protected def fromMatcher(matcher: Matcher): Scala2DependencyVersion = apply(
    version = matcher.group(2),
    scala2versionMinor = matcher.group(1)
  )

  final def apply(
    version: String,
    scala2versionMinor: String
  ): Scala2DependencyVersion = Scala2DependencyVersion(
    this,
    version,
    scala2versionMinor
  )
