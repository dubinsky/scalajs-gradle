package org.podval.tools.test

import org.podval.tools.build.{JavaDependency, ScalaDependency, ScalaPlatform, ScalaVersion, Version}

object Sbt:
  val group: String = "org.scala-sbt"

  object TestInterface extends JavaDependency.Maker:
    override def versionDefault: Version = Version("1.0")
    override def group: String = Sbt.group
    override def artifact: String = "test-interface"
    
  object Zinc extends ScalaDependency.MakerScala2Jvm:
    // Note: even with Scala 2.12 in the project, Zinc must be for 2.13, since it is used by the plugin itself;
    // Gradle Scala Plugin also requires Zinc 2.13 since version 7.5.
    override def scalaVersion(scalaPlatform: ScalaPlatform): Version = ScalaVersion.Scala2.majorAndMinor
    override def versionDefault: Version = Version("1.10.8")
    override def group: String = Sbt.group
    override def artifact: String = "zinc"
