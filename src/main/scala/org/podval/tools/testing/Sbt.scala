package org.podval.tools.testing

import org.podval.tools.build.{JavaDependency, ScalaDependency, ScalaLibrary, Version}

object Sbt:
  val group: String = "org.scala-sbt"
  val versionDefault: Version = Version("1.10.7")

  object Zinc extends ScalaDependency.Scala2(group, "zinc", isScalaJS = false)
  
  object TestInterface extends JavaDependency(group, "test-interface"):
    val versionDefault: Version = Version("1.0")

//  def main(args: Array[String]) = println(Zinc.withScalaVersion(ScalaLibrary.forVersion(ScalaLibrary.Scala3.versionDefault)))
    