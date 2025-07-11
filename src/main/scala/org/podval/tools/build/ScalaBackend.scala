package org.podval.tools.build

import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.podval.tools.gradle.{Archive, TaskWithGradleUserHomeDir, Tasks}
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
  
  protected def testTaskClass: Class[? <: TestTask[this.type]]

  def apply(project: Project, jvmPluginServices: JvmPluginServices): Unit =
    TaskWithGradleUserHomeDir.configureTasks(project)
    Tasks.configureEach(project, classOf[TestTask[this.type]], (testTask: TestTask[this.type]) =>
      testTask.setGroup(Tasks.verificationGroup)
      testTask.useSbt()
    )

  def afterEvaluate(project: Project, projectScalaLibrary: ScalaLibrary): Unit =
    val archiveAppendix: String = s"${artifactSuffixString}_${projectScalaLibrary.scalaVersion.binaryVersion.versionSuffix}"
    val pluginScalaLibrary: ScalaLibrary = ScalaLibrary.getFromClasspath

    Tasks.configure(project, classOf[Jar], Tasks.jarTaskName, (jar: Jar) =>
      Tasks.conventionProvider(
        jar,
        _.getArchiveFileName,
        Archive.noDashInFileNameBeforeAppendix,
        project
      )
      Tasks.convention(
        jar,
        _.getArchiveAppendix,
        _ => archiveAppendix
      )
    )

    dependencyRequirements(
      project,
      projectScalaVersion = projectScalaLibrary.scalaVersion,
      pluginScalaVersion = pluginScalaLibrary.scalaVersion
    ).foreach(_.apply(project))

  def registerTasks(project: Project): Unit

  protected def dependencyRequirements(
    project: Project,
    projectScalaVersion: ScalaVersion,
    pluginScalaVersion: ScalaVersion
  ): Seq[DependencyRequirement.Many]

  final def describe(what: String): String = s"$name $what."

  final def artifactSuffixString: String = artifactSuffix.map(suffix => s"_$suffix").getOrElse("")

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
  ): TaskProvider[?] =
    registerTask(
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
