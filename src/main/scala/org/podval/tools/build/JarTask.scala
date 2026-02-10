package org.podval.tools.build

import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.util.internal.GUtil
import org.podval.tools.nonjvm.NonJvmBackend
import org.podval.tools.util.Tasks

object JarTask:
  def configureTasks(
    project: Project,
    backend: NonJvmBackend
  ): Unit = Tasks.configureEach(
    project,
    classOf[Jar],
    (jar: Jar) =>
      Tasks.conventionProvider(
        project = project,
        task = jar,
        property = _.getArchiveAppendix,
        convention = jar =>
          if jar.getArchiveClassifier.isPresent
          then backend.sourceRoot
          else null
      )
  )

  def configureJarTask(
    project: Project,
    backend: Backend,
    scalaLibrary: ScalaLibrary
  ): Unit =
    val artifactSuffix: String = Artifact.suffix(backend, scalaLibrary)
    Tasks.configure(
      project,
      classOf[Jar],
      Tasks.jarTaskName,
      (jar: Jar) =>
        Tasks.conventionProvider(
          project = project,
          task = jar,
          property = _.getArchiveFileName,
          convention = noDashInFileNameBeforeAppendix
        )
        Tasks.convention(
          task = jar,
          property = _.getArchiveAppendix,
          convention = _ => artifactSuffix
        )
    )

  // The only change: no dash before the appendix.
  // [baseName][appendix]-[version]-[classifier].[extension]
  private def noDashInFileNameBeforeAppendix(jar: Jar): String =
    var name: String = GUtil.elvis(jar.getArchiveBaseName.getOrNull, "")
    name += GUtil.elvis(jar.getArchiveAppendix.getOrNull, "")
    name += maybe(name, jar.getArchiveVersion.getOrNull)
    name += maybe(name, jar.getArchiveClassifier.getOrNull)

    val extension: String = jar.getArchiveExtension.getOrNull
    name += (if GUtil.isTrue(extension) then "." + extension else "")
    name

  private def maybe(prefix: String, value: String): String =
    if !GUtil.isTrue(value) then ""
    else if !GUtil.isTrue(prefix) then value
    else "-".concat(value)
