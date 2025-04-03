package org.podval.tools.scalajsplugin.jvm

import org.gradle.api.Project
import org.podval.tools.build.{DependencyRequirement, ScalaBackend, ScalaPlatform}
import org.podval.tools.scalajsplugin.{BackendDelegate, GradleNames, TestTaskMaker}
import org.podval.tools.test.SbtTestInterface
import scala.jdk.CollectionConverters.IterableHasAsScala

final class JvmDelegate(
  project: Project,
  gradleNames: GradleNames
) extends BackendDelegate(
  project,
  gradleNames
):
  override protected def backend: ScalaBackend = ScalaBackend.Jvm

  override protected def configurationToAddToClassPath: Option[String] = None

  override protected def configureProject(isScala3: Boolean): Unit = ()

  override def setUpProject(): TestTaskMaker[JvmTestTask] = TestTaskMaker[JvmTestTask](
    gradleNames.testSourceSetName,
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
