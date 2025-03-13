package org.podval.tools.test

import org.podval.tools.build.{JavaDependency, ScalaDependency, ScalaPlatform, ScalaVersion, Version}

object Sbt:
  val group: String = "org.scala-sbt"

  object TestInterface extends JavaDependency.Maker:
    override def versionDefault: Version = Version("1.0")
    override def group: String = Sbt.group
    override def artifact: String = "test-interface"
    
  object Zinc extends ScalaDependency.MakerScala2Jvm:
    override def scalaVersion(scalaPlatform: ScalaPlatform): Version = ScalaVersion.Scala2.majorAndMinor
    override def versionDefault: Version = Version("1.10.8")
    override def group: String = Sbt.group
    override def artifact: String = "zinc"
