package org.podval.tools.build

import org.gradle.api.internal.tasks.JvmConstants
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.{Action, Project}
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.util.internal.GUtil
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
  val expandClassPathForTestEnvironment: Boolean
) derives CanEqual:

  protected def testTaskClass: Class[? <: TestTask[this.type]]

  def apply(project: Project, jvmPluginServices: JvmPluginServices): Unit =
    configureTestTasks(project)
  
  def afterEvaluate(project: Project, scalaLibrary: ScalaLibrary): Unit =
    val pluginScalaLibrary = ScalaLibrary.getFromClasspath(GradleClassPath.collect(this))

    configureJarTask(project, scalaLibrary.scalaVersion)

    dependencyRequirements(
      project,
      projectScalaVersion = scalaLibrary.scalaVersion,
      pluginScalaVersion = pluginScalaLibrary.scalaVersion
    )
      .foreach(_.apply(project))

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
  ): TaskProvider[T] =
    project.getTasks.withType(taskClass).configureEach((task: BackendTask[this.type]) =>
      task.setDescription(s"$before $name code$after.")
      task.setGroup(group)
    )

    val action: Action[T] = (task: T) => dependsOn.foreach(task.dependsOn(_))

    if !replace then
      project.getTasks.register(taskName, taskClass, action)
    else
      project.getTasks.replace(taskName, taskClass)
      project.getTasks.withType(taskClass).named(taskName, action)

  final protected def registerTestTask(
    project: Project,
    dependsOn: Option[TaskProvider[?]]
  ): TaskProvider[?] =
    registerTask(
      project,
      taskClass = testTaskClass,
      // Test task and test source set are named the same.
      taskName = SourceSets.testSourceSet(project).getName,
      before = "Tests",
      after = " using sbt frameworks",
      group = LifecycleBasePlugin.VERIFICATION_GROUP,
      dependsOn = dependsOn,
      // Replace 'test' task.
      replace = true
    )

  private def configureTestTasks(project: Project): Unit = project
    .getTasks
    .withType(classOf[TestTask[this.type]])
    .configureEach((testTask: TestTask[this.type]) =>
      testTask.setGroup(JavaBasePlugin.VERIFICATION_GROUP)
      testTask.useSbt()
    )

  private def configureJarTask(project: Project, scalaVersion: ScalaVersion): Unit =
    val jarTaskName: String = JvmConstants.JAR_TASK_NAME
    project.getTasks.withType(classOf[Jar]).named(jarTaskName).configure(
      removeDashBeforeArchiveAppendix(project)
    )

    val jarAppendix: String = s"${artifactSuffixString}_${scalaVersion.binaryVersion.versionSuffix}"
    project.getTasks.withType(classOf[Jar]).named(jarTaskName).configure((jar: Jar) =>
      jar.getArchiveAppendix.convention(jarAppendix)
    )

  private def removeDashBeforeArchiveAppendix(project: Project): Action[Jar] = (jar: Jar) => jar
    .getArchiveFileName
    .convention(project.provider(() =>
      // The only change: no dash before the appendix.
      // [baseName][appendix]-[version]-[classifier].[extension]
      var name: String = GUtil.elvis(jar.getArchiveBaseName.getOrNull, "")
      name += GUtil.elvis(jar.getArchiveAppendix.getOrNull, "")
      name += maybe(name, jar.getArchiveVersion.getOrNull)
      name += maybe(name, jar.getArchiveClassifier.getOrNull)

      val extension: String = jar.getArchiveExtension.getOrNull
      name += (if GUtil.isTrue(extension) then "." + extension else "")
      name
    ))

  private def maybe(prefix: String, value: String): String =
    if !GUtil.isTrue(value) then ""
    else if !GUtil.isTrue(prefix) then value
    else "-".concat(value)
