package org.podval.tools.scalaplugin

import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.tasks.JvmConstants
import org.gradle.api.plugins.{JavaPluginExtension, JvmTestSuitePlugin}
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.{Action, Project, Task}
import org.gradle.api.tasks.{ScalaSourceDirectorySet, SourceSet, SourceSetContainer, SourceTask}
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

  def addSources(
    project: Project,
    scalaVersion: ScalaVersion,
    sharedProject: Option[Project],
    isRunningInIntelliJ: Boolean
  ): Unit =
    val version: Version.Simple = scalaVersion.version
    val scalaRootsForVersions: Seq[String] = 1.to(version.length).map(version.take).map(version => s"scala-$version")
    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(project)

    // Add non-shared version-specific Scala sources.
    addScalaSources(project, mainSourceSet, scalaRootsForVersions)
    addScalaSources(project, testSourceSet, scalaRootsForVersions)

    sharedProject.foreach: (sharedProject: Project) =>
      val scalaRoots: Seq[String] = Seq("scala") ++ scalaRootsForVersions
      if !isRunningInIntelliJ then
        addScalaSources(sharedProject, mainSourceSet, scalaRoots)
        addScalaSources(sharedProject, testSourceSet, scalaRoots)
      else
        // Add dependency on the shared sibling.
        // TODO exclude this dependency from publications!
        project.getDependencies.add(mainSourceSet.getImplementationConfigurationName, sharedProject)

        // Add shared sources for the execution of the tasks that need them:
        // add before and remove after, so that IntelliJ does not
        // run into "duplicate content roots" issue during project import.
        val sharedScalaSourcesForTask: Action[Task] = (task: Task) =>
          task.doFirst(new Action[Task]:
            override def execute(t: Task): Unit =
              addScalaSources(sharedProject, mainSourceSet, scalaRoots)
              addScalaSources(sharedProject, testSourceSet, scalaRoots)
          )
          task.doLast(new Action[Task]:
            override def execute(t: Task): Unit =
              removeScalaSources(sharedProject, mainSourceSet, scalaRoots)
              removeScalaSources(sharedProject, testSourceSet, scalaRoots)
          )

        project.getTasks.withType(classOf[SourceTask         ]).configureEach(sharedScalaSourcesForTask)
        project.getTasks.withType(classOf[AbstractArchiveTask]).configureEach(sharedScalaSourcesForTask)

  private def scalaSources(
    project: Project,
    sourceSet: SourceSet,
    scalaRoots: Seq[String]
  ): Seq[File] = for scalaRoot: String <- scalaRoots yield Files.file(
    project.getProjectDir,
    "src",
    sourceSet.getName,
    scalaRoot
  )

  private def addScalaSources(
    project: Project,
    sourceSet: SourceSet,
    scalaRoots: Seq[String]
  ): Unit =
    val sourceDirectories: Seq[File] = scalaSources(project, sourceSet, scalaRoots)
    getScalaSourceDirectorySet(sourceSet).srcDirs(sourceDirectories *)

  private def removeScalaSources(
    project: Project,
    sourceSet: SourceSet,
    scalaRoots: Seq[String]
  ): Unit =
    val sourceDirectories: Seq[File] = scalaSources(project, sourceSet, scalaRoots)

    def toRemove(o: Object): Boolean = o.isInstanceOf[File] && {
      val file: File = o.asInstanceOf[File]
      sourceDirectories.exists(_.getAbsolutePath == file.getAbsolutePath)
    }

    val scalaSourceDirectorySet: ScalaSourceDirectorySet = getScalaSourceDirectorySet(sourceSet)
    scalaSourceDirectorySet.setSrcDirs(defaultSourceDirectorySetSource
      .get(scalaSourceDirectorySet)
      .asInstanceOf[java.util.List[Object]]
      .asScala
      .filterNot(toRemove)
      .asJava
    )

  private def getScalaSourceDirectorySet(sourceSet: SourceSet): ScalaSourceDirectorySet = sourceSet
    .getExtensions
    .getByType(classOf[ScalaSourceDirectorySet])

  private val defaultSourceDirectorySetSource: Field = classOf[DefaultSourceDirectorySet].getDeclaredField("source")
  defaultSourceDirectorySetSource.setAccessible(true)
