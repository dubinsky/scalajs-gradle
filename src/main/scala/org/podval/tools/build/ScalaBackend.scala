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
  def all: Set[ScalaBackend] = Set(JvmBackend, ScalaJSBackend, ScalaNativeBackend)

  def bySourceRoot(sourceRoot: String): Option[ScalaBackend] = all.find(_.sourceRoot == sourceRoot)
  
  def byAnyName(name: String): Option[ScalaBackend] = all.find(backend =>
    name.toLowerCase == backend.name      .toLowerCase ||
    name.toLowerCase == backend.sourceRoot.toLowerCase
  )

abstract class ScalaBackend(
  val name: String,
  val sourceRoot: String,
  val artifactSuffix: Option[String],
  val testsCanNotBeForked: Boolean,
  val expandClasspathForTestEnvironment: Boolean
) derives CanEqual:
  def isJvm   : Boolean
  def isJs    : Boolean
  def isNative: Boolean

  final override def toString: String = name
  
  final def artifactNameSuffix(version: Version): String =
    val artifactSuffixString: String = artifactSuffix.map(suffix => s"_$suffix").getOrElse("")
    s"${artifactSuffixString}_$version"

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
      archiveAppendix = artifactNameSuffix(projectScalaLibrary.scalaVersion.binaryVersion.versionSuffix)
    )

    dependencyRequirements(
      project,
      projectScalaLibrary = projectScalaLibrary,
      pluginScalaLibrary = pluginScalaLibrary
    ).foreach(_.apply(project))
  
  protected def dependencyRequirements(
    project: Project,
    projectScalaLibrary: ScalaLibrary,
    pluginScalaLibrary: ScalaLibrary
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
