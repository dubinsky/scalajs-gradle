package org.podval.tools.scalajs.dependencies

// TODO unify with Scal3Dependency, even though the Scala 3 version is always 3?
open class Scala3Dependency(
  group: String,
  nameBase: String
) extends SimpleDependency(
  group = group,
  nameBase = s"${nameBase}_3"
)
