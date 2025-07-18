package org.podval.tools.gradle

import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.util.internal.GUtil

object Archive:
  def configureAppendix(project: Project, appendix: String): Unit =
    Tasks.configureEach(
      project,
      classOf[Jar],
      Tasks.conventionProvider(
        _,
        _.getArchiveAppendix,
        jar => if jar.getArchiveClassifier.isPresent then appendix else null,
        project
      )
    )
    
  def configureJarTask(project: Project, archiveAppendix: String): Unit =
    Tasks.configure(project, classOf[Jar], Tasks.jarTaskName, (jar: Jar) =>
      Tasks.conventionProvider(
        jar,
        _.getArchiveFileName,
        noDashInFileNameBeforeAppendix,
        project
      )
      Tasks.convention(
        jar,
        _.getArchiveAppendix,
        _ => archiveAppendix
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
