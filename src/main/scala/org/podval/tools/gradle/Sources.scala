package org.podval.tools.gradle

import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.{AbstractCopyTask, ScalaSourceDirectorySet, SourceSet, SourceTask}
import org.gradle.api.{Action, Project, Task}
import org.podval.tools.build.{ScalaVersion, Version}
import org.podval.tools.util.Files
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}
import java.io.File
import java.lang.reflect.Field

object Sources:
  def removeAll(sourceSet: SourceSet): SourceDirectorySet =
    getScalaSourceDirectorySet(sourceSet).setSrcDirs(List.empty.asJava)

  def addVersionSpecific(
    project: Project,
    scalaVersion: ScalaVersion
  ): Unit =
    val version: Version = scalaVersion.version
    // TODO introduce version ranges...
    val scalaRoots: Seq[String] = 1
      .to(version.length)
      .map(version.take)
      .map(version => s"scala-$version")

    def included(file: File): Boolean = scalaRoots.contains(file.getName)

    def add(sourceSet: SourceSet): Unit =
      val src: File = Files.file(
        Projects.projectDir(project),
        "src",
        sourceSet.getName
      )
      val roots: Seq[File] = Option(src.listFiles)
        .map(_.toSeq)
        .getOrElse(Seq.empty)
        .filter(included)
      if roots.nonEmpty then getScalaSourceDirectorySet(sourceSet).srcDirs(roots *)

    add(Configurations.mainSourceSet(project))
    add(Configurations.testSourceSet(project))

  def addShared(
    shared: Project,
    project: Project,
    isRunningInIntelliJ: Boolean
  ): Unit =
    val mainSourceSet: SourceSet = Configurations.mainSourceSet(project)
    val testSourceSet: SourceSet = Configurations.testSourceSet(project)
    val sharedMainSourceSet: SourceSet = Configurations.mainSourceSet(shared)
    val sharedTestSourceSet: SourceSet = Configurations.testSourceSet(shared)

    def addBoth(): Unit =
      add(sharedMainSourceSet, mainSourceSet)
      add(sharedTestSourceSet, testSourceSet)

    def removeBoth(): Unit =
      remove(sharedMainSourceSet, mainSourceSet)
      remove(sharedTestSourceSet, testSourceSet)
      
    if !isRunningInIntelliJ then addBoth() else
      // Add dependency on the shared sibling.
      // TODO exclude this dependency from publications!
      Configurations.addDependency(project, Configurations.implementationName(project), shared)

      // Add shared sources for the execution of the tasks that need them:
      // add before and remove after, so that IntelliJ does not
      // run into "duplicate content roots" issue during project import.
      def sharedForTask(task: Task): Unit =
        // Note: task actions below *must* be Actions and not just lambdas:
        task.doFirst(new Action[Task] { override def execute(task: Task): Unit = addBoth   () }) //noinspection ConvertExpressionToSAM
        task.doLast (new Action[Task] { override def execute(task: Task): Unit = removeBoth() }) //noinspection ConvertExpressionToSAM

      Tasks.configureEach(project, classOf[SourceTask         ], sharedForTask(_)) // compilation
      Tasks.configureEach(project, classOf[AbstractArchiveTask], sharedForTask(_)) // archives
      Tasks.configureEach(project, classOf[AbstractCopyTask   ], sharedForTask(_)) // resources

  private def add(shared: SourceSet, to: SourceSet): Unit =
    def add(getSourceDirectorySet: SourceSet => SourceDirectorySet): Unit =
      getSourceDirectorySet(to).srcDirs(getSrcDirs(getSourceDirectorySet(shared)) *)

    both(add)

  private def remove(shared: SourceSet, from: SourceSet): Unit =
    def remove(getSourceDirectorySet: SourceSet => SourceDirectorySet): Unit =
      val toRemove: List[File] = getSrcDirs(getSourceDirectorySet(shared))
      val fromSourceDirectorySet = getSourceDirectorySet(from)
      fromSourceDirectorySet.setSrcDirs(getSrcDirs(fromSourceDirectorySet)
        .filterNot(file => toRemove.exists(_.getAbsolutePath == file.getAbsolutePath))
        .asJava
      )

    both(remove)

  private def both(f: (SourceSet => SourceDirectorySet) => Unit): Unit =
    f(getScalaSourceDirectorySet)
    f(_.getResources)
    
  private def getScalaSourceDirectorySet(sourceSet: SourceSet): ScalaSourceDirectorySet = sourceSet
    .getExtensions
    .getByType(classOf[ScalaSourceDirectorySet])

  private val defaultSourceDirectorySetSource: Field = classOf[DefaultSourceDirectorySet].getDeclaredField("source")
  defaultSourceDirectorySetSource.setAccessible(true)

  private def getSrcDirs(sourceDirectorySet: SourceDirectorySet): List[File] = defaultSourceDirectorySetSource
    .get(sourceDirectorySet)
    .asInstanceOf[java.util.List[Object]]
    .asScala
    .toList
    .filter(_.isInstanceOf[File])
    .map   (_.asInstanceOf[File])
