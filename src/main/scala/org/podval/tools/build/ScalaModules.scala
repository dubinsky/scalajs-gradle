package org.podval.tools.build

import org.podval.tools.backend.ScalaBackend
import org.podval.tools.backend.jvm.JvmBackend

object ScalaModules:
  object ParallelCollections extends ScalaDependency.Maker:
    def group: String = "org.scala-lang.modules"
    def artifact: String = "scala-parallel-collections"
    def versionDefault: Version.Simple = Version.Simple("1.2.0")
    override def description: String = "Scala Module: Parallel Collections."
    override def scalaBackend: ScalaBackend = JvmBackend
    override def scala2: Boolean = true
