package org.podval.tools.build

object ScalaModules:
  object ParallelCollections extends ScalaDependency.MakerScala2Jvm:
    def group: String = "org.scala-lang.modules"
    def artifact: String = "scala-parallel-collections"
    def versionDefault: Version = Version("1.2.0")
    override def description: String = "Scala Module: Parallel Collections."
