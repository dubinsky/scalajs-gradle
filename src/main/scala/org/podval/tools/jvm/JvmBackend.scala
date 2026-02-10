package org.podval.tools.jvm

import org.gradle.api.Project
import org.podval.tools.build.{Backend, JavaDependency, Requirement, ScalaLibrary, Version}
import org.podval.tools.gradle.Configurations
import org.podval.tools.test.framework.Framework
import org.podval.tools.test.task.TestEnvironment

object JvmBackend extends Backend(
  name = "JVM",
  sourceRoot = "jvm",
  artifactSuffix = None,
  testsCanNotBeForked = false,
  expandClasspathForTestEnvironment = true
) with TestEnvironment.Creator[JvmBackend.type]:
  override protected def testTaskClass: Class[JvmTestTask] = classOf[JvmTestTask]
  
  override def registerTasks(project: Project): Unit =
    registerTestTask(project, dependsOn = None)

  override protected def requirements(
    project: Project,
    projectScalaLibrary: ScalaLibrary,
    pluginScalaLibrary: ScalaLibrary
  ): Seq[Requirement.Many] = Seq(
    Requirement.Many(
      configurationName = Configurations.testRuntimeOnlyName(project),
      scalaLibrary = projectScalaLibrary,
      requirements = Array(
        SbtTestInterface.require()
      )
    )
  )

  object SbtTestInterface extends JavaDependency(
    name = "SBT testing interface; some test frameworks (ScalaTest :)) do not bring it in.",
    group = "org.scala-sbt",
    versionDefault = Version("1.0"),
    artifact = "test-interface"
  )

  override def testEnvironment: TestEnvironment[JvmBackend.type] = new TestEnvironment[JvmBackend.type](
    backend = this,
    sourceMapper = None
  ):
    final override def close(): Unit = ()
    final override protected def loadFrameworks: List[Framework.Loaded] = frameworks.flatMap(_.tryLoad)
