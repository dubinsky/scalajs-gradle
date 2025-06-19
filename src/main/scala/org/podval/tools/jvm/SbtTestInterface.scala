package org.podval.tools.jvm

import org.podval.tools.build.{JavaDependencyMaker, Version}

object SbtTestInterface extends JavaDependencyMaker:
  override def group: String = "org.scala-sbt"
  override def artifact: String = "test-interface"
  override def versionDefault: Version = Version("1.0")
  override def description: String = "SBT testing interface; some test frameworks (ScalaTest :)) do not bring it in."
