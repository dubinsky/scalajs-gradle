package org.podval.tools.backend

import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.tasks.JvmConstants
import org.gradle.api.plugins.{JavaPluginExtension, JvmTestSuitePlugin}
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.{Action, Project, Task}
import org.gradle.api.tasks.{AbstractCopyTask, ScalaSourceDirectorySet, SourceSet, SourceSetContainer, SourceTask}
import org.podval.tools.build.{ScalaVersion, Version}
import org.podval.tools.util.Files
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}
import java.io.File
import java.lang.reflect.Field

object Sources:
  def getSourceSets(project: Project): (SourceSet, SourceSet) =
    val sourceSets: SourceSetContainer = project.getExtensions.getByType(classOf[JavaPluginExtension]).getSourceSets
    (
      sourceSets.getByName(JvmConstants.JAVA_MAIN_FEATURE_NAME),
      sourceSets.getByName(JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME)
    )

  def getSourceSet(project: Project, isTest: Boolean): SourceSet =
    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(project)
    if isTest
    then testSourceSet
    else mainSourceSet

  def clearSourceSet(sourceSet: SourceSet): SourceDirectorySet =
    getScalaSourceDirectorySet(sourceSet).setSrcDirs(List.empty.asJava)

  def addVersionSpecificScalaSources(
    project: Project,
    scalaVersion: ScalaVersion
  ): Unit =
    val version: Version = scalaVersion.version
    val scalaRoots: Seq[String] = 1
      .to(version.length)
      .map(version.take)
      .map(version => s"scala-$version")
    
    def addScalaRoots(sourceSet: SourceSet): Unit =
      val sourceDirectories: Seq[File] = scalaRoots.map(scalaRoot => Files.file(
        project.getProjectDir,
        "src",
        sourceSet.getName,
        scalaRoot
      ))
      getScalaSourceDirectorySet(sourceSet).srcDirs(sourceDirectories *)

    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(project)
    addScalaRoots(mainSourceSet)
    addScalaRoots(testSourceSet)

  def addSharedSources(
    project: Project,
    shared: Project,
    isRunningInIntelliJ: Boolean
  ): Unit =
    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(project)
    val (sharedMainSourceSet: SourceSet, sharedTestSourceSet: SourceSet) = getSourceSets(shared)

    def addBoth(): Unit =
      add(sharedMainSourceSet, mainSourceSet)
      add(sharedTestSourceSet, testSourceSet)

    def removeBoth(): Unit =
      remove(sharedMainSourceSet, mainSourceSet)
      remove(sharedTestSourceSet, testSourceSet)
      
    if !isRunningInIntelliJ then addBoth() else
      // Add dependency on the shared sibling.
      // TODO exclude this dependency from publications!
      project.getDependencies.add(mainSourceSet.getImplementationConfigurationName, shared)
      
      // Add shared sources for the execution of the tasks that need them:
      // add before and remove after, so that IntelliJ does not
      // run into "duplicate content roots" issue during project import.
      val sharedForTask: Action[Task] = (task: Task) =>
        // Note: task actions below *must* be Actions and not just lambdas:
        task.doFirst(new Action[Task] { override def execute(task: Task): Unit = addBoth   () }) //noinspection ConvertExpressionToSAM
        task.doLast (new Action[Task] { override def execute(task: Task): Unit = removeBoth() }) //noinspection ConvertExpressionToSAM

      project.getTasks.withType(classOf[SourceTask         ]).configureEach(sharedForTask) // compilation
      project.getTasks.withType(classOf[AbstractArchiveTask]).configureEach(sharedForTask) // archives
      project.getTasks.withType(classOf[AbstractCopyTask   ]).configureEach(sharedForTask) // resources

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

  private def getSrcDirs(
    scalaSourceDirectorySet: SourceDirectorySet
  ): List[File] = defaultSourceDirectorySetSource
    .get(scalaSourceDirectorySet)
    .asInstanceOf[java.util.List[Object]]
    .asScala
    .toList
    .filter(_.isInstanceOf[File])
    .map   (_.asInstanceOf[File])
