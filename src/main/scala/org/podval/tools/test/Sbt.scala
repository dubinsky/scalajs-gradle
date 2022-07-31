package org.podval.tools.test

import org.opentorah.build.Scala2Dependency

object Sbt:
  val configurationName: String = "sbt"

  val group: String = "org.scala-sbt"

  object ZincPersist extends Scala2Dependency(group = group, nameBase = "zinc-persist")
