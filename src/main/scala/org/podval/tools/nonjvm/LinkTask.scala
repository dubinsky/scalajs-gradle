package org.podval.tools.nonjvm

import org.gradle.api.{DefaultTask, Project}
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.{CacheableTask, Classpath, InputFiles, Internal, SourceSet, TaskAction}
import org.podval.tools.build.{Backend, OutputTask}
import org.podval.tools.util.{Configurations, Files, Projects, Tasks}
import scala.jdk.CollectionConverters.SetHasAsScala
import java.io.File

@CacheableTask
abstract class LinkTask[B <: NonJvmBackend] extends DefaultTask
  with Backend.Task[B]
  with OutputTask:

  @Internal def isTest: Boolean

  @InputFiles @Classpath def getRuntimeClasspath: ConfigurableFileCollection
  final def runtimeClasspath: Seq[File] = getRuntimeClasspath.getFiles.asScala.toSeq
 
  @TaskAction final def execute(): Unit = link.link()
  def link: Link[B]

  private val buildDirectory: File = Projects.buildDirectoryFile(getProject)
  final protected def outputDirectory: File = Files.file(buildDirectory, "tmp", getName)
  final protected def outputFile(name: String): File = File(outputDirectory, name)

object LinkTask:
  def configureTasks(project: Project): Unit = Tasks.configureEach(
    project,
    classOf[LinkTask[?]],
    (task: LinkTask[?]) =>
      val sourceSet: SourceSet = Configurations.sourceSet(project, task.isTest)
      task.dependsOn(project.getTasks.named(sourceSet.getClassesTaskName))
      task.getRuntimeClasspath.setFrom(sourceSet.getRuntimeClasspath)
  )

  @CacheableTask
  abstract class Main[B <: NonJvmBackend] extends LinkTask[B]:
    final override def isTest: Boolean = false

  @CacheableTask
  abstract class Test[B <: NonJvmBackend] extends LinkTask[B]:
    final override def isTest: Boolean = true
