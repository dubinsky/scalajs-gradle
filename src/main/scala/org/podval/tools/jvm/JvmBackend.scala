package org.podval.tools.jvm

import org.gradle.api.Project
import org.podval.tools.build.{DependencyRequirement, ScalaBackend, ScalaVersion, TestEnvironment}
import org.podval.tools.gradle.Configurations
import org.podval.tools.test.framework.FrameworkProvider
import sbt.testing.Framework

object JvmBackend extends ScalaBackend(
  name = "JVM",
  sourceRoot = "jvm",
  artifactSuffix = None,
  testsCanNotBeForked = false,
  expandClasspathForTestEnvironment = true
) with TestEnvironment.Creator[JvmBackend.type]:

  override protected def testTaskClass: Class[JvmTestTask] = classOf[JvmTestTask]
  
  override def registerTasks(project: Project): Unit =
    registerTestTask(project, dependsOn = None)

  override protected def dependencyRequirements(
    project: Project,
    projectScalaVersion: ScalaVersion,
    pluginScalaVersion: ScalaVersion
  ): Seq[DependencyRequirement.Many] = Seq(
    DependencyRequirement.Many(
      configurationName = Configurations.testRuntimeOnlyName(project),
      scalaVersion = projectScalaVersion,
      dependencyRequirements = Array(
        JvmDependency.SbtTestInterface.required()
      )
    )
  )

  override def testEnvironment: TestEnvironment[JvmBackend.type] = new TestEnvironment[JvmBackend.type](
    backend = this,
    sourceMapper = None
  ):
    override def close(): Unit = ()

    override protected def loadFrameworks: List[Framework] =
      frameworksToLoad(FrameworkProvider(_).frameworkOpt).flatten
