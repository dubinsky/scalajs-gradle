package org.podval.tools.build

object ScalaParallelCollectionsModule extends ScalaDependency.MakerScala2Jvm:
  def group: String = "org.scala-lang.modules"
  def artifact: String = "scala-parallel-collections"
  def versionDefault: Version = Version("1.2.0")
