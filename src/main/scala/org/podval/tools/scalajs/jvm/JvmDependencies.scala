package org.podval.tools.scalajs.jvm

import org.gradle.api.plugins.JavaPlugin
import org.podval.tools.build.{JavaDependency, Version}

object JvmDependencies:
  // TODO  zinc "org.scala-sbt:zinc_$scala2versionMinor"
  // TODO move under import org.podval.tools.testing
  object Zinc:
    val versionDefault: Version = Version("1.10.7")

  object SbtTestInterface extends JavaDependency(group = "org.scala-sbt", artifact = "test-interface"):
    val versionDefault: Version = Version("1.0")

  val sbtTestInterfaceDependencyRequirement = JavaDependency.Requirement(
    dependency = SbtTestInterface,
    version = SbtTestInterface.versionDefault,
    reason =
      """because some test frameworks (ScalaTest :)) do not bring it in in,
        |and it needs to be on the testImplementation classpath
        |""".stripMargin,
    configurationName = JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
  )
