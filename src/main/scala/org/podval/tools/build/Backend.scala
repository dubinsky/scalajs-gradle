package org.podval.tools.build

import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.{Project, Task as GTask}
import org.gradle.api.tasks.TaskProvider
import org.podval.tools.util.Tasks

object Backend:
  trait Task[B <: Backend] extends GTask

  val property: String = "org.podval.tools.backend"

  def all: Set[Backend] = Set(
    org.podval.tools.jvm.JvmBackend,
    org.podval.tools.scalajs.ScalaJSBackend,
    org.podval.tools.scalanative.ScalaNativeBackend
  )

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
    OutputTask.configureTasks(project, isRunningInIntelliJ)
    TestTask.configureTasks(project, testTaskClass)

  // Jar names are applied from a follow-up afterEvaluate.
  // A build-script afterEvaluate on this project can still set useArtifactSuffix,
  // and the name is in place before Gradle freezes a project-dependency File.
  // Publication ids stay on projectsEvaluated: a later afterEvaluate can still setArtifactId.
  def jarArtifactSuffix(
    projectScalaLibrary: ScalaLibrary,
    useArtifactSuffix: Boolean
  ): String =
    if useArtifactSuffix then Artifact.suffix(this, projectScalaLibrary) else ""

  def configureJarArtifacts(project: Project, artifactSuffix: String): Unit =
    JarTask.configureJarTask(project, artifactSuffix)

  def configurePublishedCoordinates(project: Project, artifactSuffix: String): Unit =
    Artifact.configureMavenPublications(
      project,
      project.getName,
      artifactSuffix
    )

  def afterEvaluate(
    project: Project,
    projectScalaLibrary: ScalaLibrary,
    pluginScalaLibrary: ScalaLibrary
  ): Unit =
    val scala3: Boolean = projectScalaLibrary.scalaVersion.binaryVersion.isScala3
    Tasks.configureEach(
      project,
      classOf[TestTask[?]],
      (task: TestTask[?]) => task.getScala3.set(scala3)
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
  ): Seq[DependencyRequirement.Many]

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
