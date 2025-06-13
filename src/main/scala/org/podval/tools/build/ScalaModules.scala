package org.podval.tools.build

import org.podval.tools.backend.jvm.JvmBackend

object ScalaModules:
  // Scala.js Linker uses it but somehow does not bring it onto the classpath.
  object ParallelCollections extends ScalaDependencyMaker:
    override def group: String = "org.scala-lang.modules"
    override def artifact: String = "scala-parallel-collections"
    override def versionDefault: Version = Version("1.2.0")
    override def description: String = "Scala Module: Parallel Collections."
    override def scalaBackend: ScalaBackend = JvmBackend
