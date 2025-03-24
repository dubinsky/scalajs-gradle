package org.podval.tools.test

import org.podval.tools.build.{JavaDependency, Version}

object SbtTestInterface extends JavaDependency.Maker:
  override def group: String = "org.scala-sbt"
  override def artifact: String = "test-interface"
  override def versionDefault: Version = Version("1.0")
