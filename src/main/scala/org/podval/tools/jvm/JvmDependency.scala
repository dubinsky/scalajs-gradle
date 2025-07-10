package org.podval.tools.jvm

import org.podval.tools.build.{JavaDependencyMaker, Version}

object JvmDependency:
  object SbtTestInterface extends JavaDependencyMaker:
    override val group: String = "org.scala-sbt"
    override val artifact: String = "test-interface"
    override val versionDefault: Version = Version("1.0")
    override val description: String = "SBT testing interface; some test frameworks (ScalaTest :)) do not bring it in."
