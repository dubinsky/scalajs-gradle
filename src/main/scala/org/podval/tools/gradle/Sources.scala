package org.podval.tools.gradle

import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.{AbstractCopyTask, ScalaSourceDirectorySet, SourceSet, SourceTask}
import org.gradle.api.{Action, Project, Task}
import org.podval.tools.build.{ScalaVersion, Version}
import org.podval.tools.platform.{Files, Strings}
import scala.jdk.CollectionConverters.{ListHasAsScala, SetHasAsScala}
import java.io.File
import java.lang.reflect.Field

object Sources:
  def exist(project: Project): Boolean = src(project).exists

  private def src(project: Project) = File(Projects.projectDir(project), "src")

  // TODO introduce version ranges?
  def addVersionSpecific(
    project: Project,
    scalaVersion: ScalaVersion
  ): Unit =
    val version: Version = scalaVersion.version
    val versionSuffixes: Seq[String] = 1
      .to(version.length)
      .map(version.take)
      .map(_.toString)

    add(project,
      (
        sourceSetGetter: Project => SourceSet,
        prefix: String,
        _: SourceSet => SourceDirectorySet
      ) => Files
        .listDirectories(File(src(project), sourceSetGetter(project).getName))
        .filter(file => Strings
          .detectPrefix(file.getName, s"$prefix-")
          .exists(versionSuffixes.contains)
        )
    )

  def addShared(
    shared: Project,
    project: Project,
    isRunningInIntelliJ: Boolean
  ): Unit =
    def add(): Unit = this.add(project,
      (
        sourceSetGetter: Project => SourceSet,
        _: String,
        directorySetGetter: SourceSet => SourceDirectorySet
      ) => defaultSourceDirectorySetSource
        .get(directorySetGetter(sourceSetGetter(shared)))
        .asInstanceOf[java.util.List[Object]]
        .asScala
        .toSeq
        .filter(_.isInstanceOf[File])
        .map   (_.asInstanceOf[File])
    )

    if !isRunningInIntelliJ then add() else
      // Add dependency on the shared sibling.
      Configurations.addDependency(project, Configurations.implementationName(project), shared)

      // Add shared sources before the execution of the tasks that need them and not at start
      // so that IntelliJ does not run into "duplicate content roots" issue during project import;
      // we do not bother to remove them after the execution of the task since
      // project import already happened and adding is done in idempotent way.
      Set(
        classOf[SourceTask         ], // compilation
        classOf[AbstractArchiveTask], // archives
        classOf[AbstractCopyTask   ] // resources
      ).foreach(Tasks.configureEach(
        project,
        _,
        // Note: task action below *must* be Action and not just lambda:
        _.doFirst(new Action[Task] { override def execute(task: Task): Unit = //noinspection ConvertExpressionToSAM
          // TODO remove Configurations.addDependency(project, Configurations.implementationName(project), shared)
          add()
        })
      ))

  private val defaultSourceDirectorySetSource: Field = classOf[DefaultSourceDirectorySet].getDeclaredField("source")
  defaultSourceDirectorySetSource.setAccessible(true)

  private def add(
    project: Project,
    f: (Project => SourceSet, String, SourceSet => SourceDirectorySet) => Seq[File]
  ): Unit =
    for
      sourceSetGetter <- Set[Project => SourceSet](
        Configurations.mainSourceSet,
        Configurations.testSourceSet
      )
      (prefix, directorySetGetter) <- Set[(String, SourceSet => SourceDirectorySet)](
        ("scala"    , _.getExtensions.getByType(classOf[ScalaSourceDirectorySet])),
        ("resources", _.getResources)
      )
    do
      // Note: idempotent: adds only directories that are not yet there
      // to obviate the need to remove anything
      // and to accommodate adding shared directories manually
      val directorySet: SourceDirectorySet = directorySetGetter(sourceSetGetter(project))
      val existing: Set[File] = directorySet
        .getSrcDirs
        .asScala
        .toSet
      f(sourceSetGetter, prefix, directorySetGetter)
        .filterNot(existing.contains)
        .foreach(directorySet.srcDir)
