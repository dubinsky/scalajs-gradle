package org.podval.tools.testing

import org.podval.tools.build.{JavaDependency, ScalaDependency, ScalaPlatform, Version}

object Sbt:
  val group: String = "org.scala-sbt"
  val versionDefault: Version = Version("1.10.7")

  val Zinc: ScalaDependency = ScalaPlatform.Scala2.Jvm.dependency(group, "zinc")

  object TestInterface:
    val dependency: JavaDependency = JavaDependency(group, "test-interface")
    val versionDefault: Version = Version("1.0")
