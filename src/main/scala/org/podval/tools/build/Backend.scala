package org.podval.tools.build

import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.{Project, Task as GTask}
import org.gradle.api.tasks.TaskProvider
import org.podval.tools.gradle.{Archive, Tasks}
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.scalajs.ScalaJSBackend
import org.podval.tools.scalanative.ScalaNativeBackend
import org.podval.tools.task.{TaskWithInstaller, TaskWithOutput}
import org.podval.tools.test.task.TestTask

object Backend:
  trait Task[B <: Backend] extends GTask

  val property: String = "org.podval.tools.backend"

  def all: Set[Backend] = Set(JvmBackend, ScalaJSBackend, ScalaNativeBackend)

abstract class Backend(
  val name: String,
  val sourceRoot: String,
  val artifactSuffix: Option[String],
  val testsCanNotBeForked: Boolean,
  val expandClasspathForTestEnvironment: Boolean
) derives CanEqual:
  final override def toString: String = name

  final def fullName: String = s"$name ($sourceRoot)"

  protected def testTaskClass: Class[? <: TestTask[this.type]]

  def apply(
    project: Project,
    jvmPluginServices: JvmPluginServices,
    isRunningInIntelliJ: Boolean
  ): Unit =
    TaskWithInstaller.configureTasks(project)
    TaskWithOutput.configureTasks(project, isRunningInIntelliJ)
    TestTask.configureTasks(project, testTaskClass)

  def afterEvaluate(
    project: Project,
    projectScalaLibrary: ScalaLibrary,
    pluginScalaLibrary: ScalaLibrary
  ): Unit =
    Archive.configureJarTask(
      project, 
      archiveAppendix = Artifact.suffix(this, projectScalaLibrary)
    )

    requirements(
      project,
      projectScalaLibrary = projectScalaLibrary,
      pluginScalaLibrary  = pluginScalaLibrary
    ).foreach(_.apply(project))
  
  protected def requirements(
    project: Project,
    projectScalaLibrary: ScalaLibrary,
    pluginScalaLibrary: ScalaLibrary
  ): Seq[Requirement.Many]

  def registerTasks(project: Project): Unit

  final protected def registerTask[T <: Backend.Task[this.type]](
    project: Project,
    taskClass: Class[T],
    taskName: String,
    before: String,
    after: String,
    group: String,
    dependsOn: Option[TaskProvider[?]] = None,
    replace: Boolean = false
  ): TaskProvider[T] = Tasks.register(
    project,
    taskClass,
    taskName,
    description = s"$before $name code$after.",
    group,
    dependsOn,
    replace
  )

  final protected def registerTestTask(
    project: Project,
    dependsOn: Option[TaskProvider[?]]
  ): TaskProvider[?] = registerTask(
    project,
    taskClass = testTaskClass,
    taskName = Tasks.testTaskName(project),
    before = "Tests",
    after = " using sbt frameworks",
    group = Tasks.verificationGroup,
    dependsOn = dependsOn,
    // Replace 'test' task.
    replace = true
  )
