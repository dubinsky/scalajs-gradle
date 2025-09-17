package org.podval.tools.jvm

import org.gradle.api.Project
import org.podval.tools.build.{DependencyRequirement, JavaDependency, ScalaBackend, ScalaLibrary, Version}
import org.podval.tools.gradle.Configurations
import org.podval.tools.test.framework.Framework
import org.podval.tools.test.task.TestEnvironment

object JvmBackend extends ScalaBackend(
  name = "JVM",
  sourceRoot = "jvm",
  artifactSuffix = None,
  testsCanNotBeForked = false,
  expandClasspathForTestEnvironment = true
) with TestEnvironment.Creator[JvmBackend.type]:
  override def isJvm   : Boolean = true
  override def isJs    : Boolean = false
  override def isNative: Boolean = false

  override protected def testTaskClass: Class[JvmTestTask] = classOf[JvmTestTask]
  
  override def registerTasks(project: Project): Unit =
    registerTestTask(project, dependsOn = None)

  override protected def dependencyRequirements(
    project: Project,
    projectScalaLibrary: ScalaLibrary,
    pluginScalaLibrary: ScalaLibrary
  ): Seq[DependencyRequirement.Many] = Seq(
    DependencyRequirement.Many(
      configurationName = Configurations.testRuntimeOnlyName(project),
      scalaLibrary = projectScalaLibrary,
      dependencyRequirements = Array(
        sbtTestInterface.required()
      )
    )
  )

  val sbtTestInterfaceVersion: Version = Version("1.0")
  private def sbtTestInterface: JavaDependency = new JavaDependency:
    override val group: String = "org.scala-sbt"
    override val artifact: String = "test-interface"
    override val versionDefault: Version = sbtTestInterfaceVersion
    override val description: String = "SBT testing interface; some test frameworks (ScalaTest :)) do not bring it in."

  override def testEnvironment: TestEnvironment[JvmBackend.type] = new TestEnvironment[JvmBackend.type](
    backend = this,
    sourceMapper = None
  ):
    final override def close(): Unit = ()
    final override protected def loadFrameworks: List[Framework.Loaded] = frameworks.flatMap(_.tryLoad)
