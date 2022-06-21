package org.podval.tools.scalajs.dependencies

import java.util.regex.Matcher

final class SimpleDependencyVersion(
  dependency: SimpleDependency,
  version: String
) extends DependencyVersion(
  dependency,
  version
):
  final override def nameDependencyNotation: String = dependency.nameBase

open class SimpleDependency(
  group: String,
  nameBase: String
) extends Dependency(
  group = group,
  nameBase = nameBase
):

  final override protected def namePattern: String = nameBase

  final override protected def fromMatcher(matcher: Matcher, version: String): SimpleDependencyVersion = apply(
    version = version
  )

  final override protected def fromMatcher(matcher: Matcher): SimpleDependencyVersion = apply(
    version = matcher.group(1)
  )

  final def apply(
    version: String
  ): SimpleDependencyVersion = SimpleDependencyVersion(
    dependency = this,
    version = version
  )
