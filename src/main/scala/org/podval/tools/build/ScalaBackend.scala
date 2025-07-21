package org.podval.tools.build

import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.podval.tools.gradle.{Archive, TaskWithGradleUserHomeDir, TaskWithOutput, Tasks}
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.scalajs.ScalaJSBackend
import org.podval.tools.scalanative.ScalaNativeBackend
import org.podval.tools.test.task.TestTask

object ScalaBackend:
  val sharedSourceRoot: String = "shared"
  def all: Set[ScalaBackend] = Set(JvmBackend, ScalaJSBackend, ScalaNativeBackend)
  def names: String = all.map(backend => s"${backend.name} (${backend.sourceRoot})").mkString(", ")
  def sourceRoots: String = all.map(_.sourceRoot).mkString(", ")

abstract class ScalaBackend(
  val name: String,
  val sourceRoot: String,
  val artifactSuffix: Option[String],
  val testsCanNotBeForked: Boolean,
  val expandClasspathForTestEnvironment: Boolean
) derives CanEqual:

  final override def toString: String = name

  final def describe(what: String): String = s"$name $what."

  final def artifactSuffixString: String = artifactSuffix.map(suffix => s"_$suffix").getOrElse("")

  protected def testTaskClass: Class[? <: TestTask[this.type]]

  def apply(
    project: Project,
    jvmPluginServices: JvmPluginServices,
    isRunningInIntelliJ: Boolean
  ): Unit =
    TaskWithGradleUserHomeDir.configureTasks(project)
    TaskWithOutput.configureTasks(project, isRunningInIntelliJ)
    TestTask.configureTasks(project, testTaskClass)

  def afterEvaluate(
    project: Project,
    projectScalaLibrary: ScalaLibrary,
    pluginScalaLibrary: ScalaLibrary
  ): Unit =
    Archive.configureJarTask(
      project, 
      archiveAppendix = s"${artifactSuffixString}_${projectScalaLibrary.scalaVersion.binaryVersion.versionSuffix}"
    )

    dependencyRequirements(
      project,
      projectScalaVersion = projectScalaLibrary.scalaVersion,
      pluginScalaVersion = pluginScalaLibrary.scalaVersion
    ).foreach(_.apply(project))
  
  protected def dependencyRequirements(
    project: Project,
    projectScalaVersion: ScalaVersion,
    pluginScalaVersion: ScalaVersion
  ): Seq[DependencyRequirement.Many]

  def registerTasks(project: Project): Unit

  final protected def registerTask[T <: BackendTask[this.type]](
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
