package org.podval.tools.scalajsplugin.jvm

import org.gradle.api.Project
import org.podval.tools.build.{JavaDependency, ScalaBackendKind, ScalaPlatform, Version}
import org.podval.tools.scalajsplugin.{AddTestTask, BackendDelegate, GradleNames}

object Jvm extends BackendDelegate:
  override def backendKind: ScalaBackendKind = ScalaBackendKind.JVM
  override def isCreateForMixedMode: Boolean = false
  override def sourceRoot: String = "jvm"
  override def gradleNamesSuffix: String = ""
  override def pluginDependenciesConfigurationNameOpt: Option[String] = None
  override def createExtensions(project: Project): Unit = ()
  override def scalaCompileParameters(isScala3: Boolean): Seq[String] = Seq.empty

  override def addTasks(
    project: Project,
    gradleNames: GradleNames
  ): AddTestTask[JvmTestTask] =
    AddTestTask[JvmTestTask](
      classOf[JvmTestTask],
      (_: JvmTestTask) => ()
    )

  override def applyDependencyRequirements(
    project: Project,
    gradleNames: GradleNames,
    pluginScalaPlatform: ScalaPlatform,
    projectScalaPlatform: ScalaPlatform,
    isScala3: Boolean
  ): Unit =
    BackendDelegate.applyDependencyRequirements(
      project,
      Seq(Jvm.SbtTestInterface.required()),
      projectScalaPlatform,
      gradleNames.testImplementationConfigurationName
    )
  
  object SbtTestInterface extends JavaDependency.Maker:
    override def group: String = "org.scala-sbt"
    override def artifact: String = "test-interface"
    override def versionDefault: Version = Version("1.0")
    override def description: String = "SBT testing interface; some test frameworks (ScalaTest :)) do not bring it in in."
