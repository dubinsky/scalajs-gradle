package org.podval.tools.backend

import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.{ScalaSourceDirectorySet, SourceSet}
import org.podval.tools.build.{ScalaVersion, Version}
import org.podval.tools.util.{Configurations, Extensions, Files, Strings}
import scala.jdk.CollectionConverters.SetHasAsScala
import java.io.File

abstract class SingleProject(project: Project) extends BackendProject(project):
  // TODO introduce version ranges?
  final def addVersionSpecificSources(scalaVersion: ScalaVersion): Unit = project.afterEvaluate: (project: Project) =>
    val version: Version = scalaVersion.version
    val versionSuffixes: Seq[String] = 1
      .to(version.length)
      .map(version.take)
      .map(_.toString)

    addSources(
      (
        sourceSetGetter: Project => SourceSet,
        prefix: String,
        _: SourceSet => SourceDirectorySet
      ) => Files
        .listDirectories(File(srcDirectory, sourceSetGetter(project).getName))
        .filter(file => Strings
          .detectPrefix(file.getName, s"$prefix-")
          .exists(versionSuffixes.contains)
        )
    )  

  final protected def addSources(
    f: (
      Project => SourceSet,
      String,
      SourceSet => SourceDirectorySet
    ) => Seq[File]
  ): Unit =
    for
      sourceSetGetter <- Set[Project => SourceSet](
        Configurations.mainSourceSet,
        Configurations.testSourceSet
      )
      (prefix, directorySetGetter) <- Set[(String, SourceSet => SourceDirectorySet)](
        ("scala"    , Extensions.getByType(_, classOf[ScalaSourceDirectorySet])),
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
