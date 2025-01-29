package org.podval.tools.scalajs.jvm

import org.gradle.api.plugins.JavaPlugin
import org.podval.tools.build.{JavaDependency, Version}
import org.podval.tools.testing.Sbt

object JvmDependencies:
  object SbtTestInterfaceDependencyRequirement extends JavaDependency.Requirement(
    dependency = Sbt.TestInterface,
    version = Sbt.TestInterface.versionDefault,
    reason =
      """because some test frameworks (ScalaTest :)) do not bring it in in,
        |and it needs to be on the testImplementation classpath
        |""".stripMargin,
    configurationName = JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
  )
