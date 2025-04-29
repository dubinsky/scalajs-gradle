package org.podval.tools.scalajsplugin.jvm

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.podval.tools.build.{DependencyRequirement, ScalaPlatform}
import org.podval.tools.scalajsplugin.{AddTestTask, BackendDelegate, BackendDelegateKind}
import org.podval.tools.test.SbtTestInterface
import scala.jdk.CollectionConverters.IterableHasAsScala

final class JvmDelegate(
  project: Project,
  isModeMixed: Boolean
) extends BackendDelegate(
  project,
  isModeMixed
):
  override protected def kind: BackendDelegateKind = BackendDelegateKind.JVM
  
  override protected def configurationToAddToClassPath: Option[String] = None

  override protected def configureProject(isScala3: Boolean): Unit = ()

  override protected def setUpProject(): AddTestTask[JvmTestTask] = AddTestTask[JvmTestTask](
    gradleNames.testSourceSetName,
    gradleNames.testTaskName,
    classOf[JvmTestTask],
    (_: JvmTestTask) => ()
  )

  override protected def dependencyRequirements(
    pluginScalaPlatform: ScalaPlatform,
    projectScalaPlatform: ScalaPlatform
  ): Seq[DependencyRequirement] = Seq(
    SbtTestInterface.required(
      platform = projectScalaPlatform,
      reason =
        """because some test frameworks (ScalaTest :)) do not bring it in in,
          |and it needs to be on the testImplementation classpath
          |""".stripMargin,
      configurationName = gradleNames.testImplementationConfigurationName
    )
  )
