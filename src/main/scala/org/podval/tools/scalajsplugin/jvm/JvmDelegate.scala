package org.podval.tools.scalajsplugin.jvm

import org.gradle.api.Project
import org.podval.tools.build.{ScalaBackendKind, ScalaLibrary, ScalaPlatform}
import org.podval.tools.scalajsplugin.{AddTestTask, AddToClassPath, BackendDelegate, BackendDelegateKind}
import org.podval.tools.test.SbtTestInterface

object JvmDelegate extends BackendDelegateKind(
  sourceRoot = "jvm",
  backendKind = ScalaBackendKind.JVM,
  mk = JvmDelegate.apply
)

final class JvmDelegate(
  project: Project,
  isModeMixed: Boolean
) extends BackendDelegate(
  project,
  isModeMixed
):
  override protected def kind: BackendDelegateKind = JvmDelegate

  override protected def gradleNamesSuffix: String = ""
  
  override protected def isCreateForMixedMode: Boolean = false

  override protected def createConfigurations(): Unit = ()

  override protected def setUpProject(): AddTestTask[JvmTestTask] = AddTestTask[JvmTestTask](
    classOf[JvmTestTask],
    (_: JvmTestTask) => ()
  )

  override protected def afterEvaluate(
    pluginScalaPlatform: ScalaPlatform,
    projectScalaLibrary: ScalaLibrary,
    projectScalaPlatform: ScalaPlatform
  ): Option[AddToClassPath] =
    applyDependencyRequirements(
      Seq(SbtTestInterface.required()),
      projectScalaPlatform,
      gradleNames.testImplementationConfigurationName
    )
    
    None
    