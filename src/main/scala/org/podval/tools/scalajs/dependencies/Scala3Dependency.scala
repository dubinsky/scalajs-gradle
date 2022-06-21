package org.podval.tools.scalajs.dependencies

open class Scala3Dependency(
  group: String,
  nameBase: String
) extends SimpleDependency(
  group = group,
  nameBase = s"${nameBase}_3"
)
