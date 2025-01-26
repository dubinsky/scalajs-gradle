package org.podval.tools.build

import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.{Classpath, SourceSet}
import org.podval.tools.build.Gradle.*

trait TaskWithSourceSet extends Task:
  protected final def sourceSet: SourceSet = getProject.getSourceSet(sourceSetName)

  protected def sourceSetName: String

  @Classpath final def getRuntimeClassPath: FileCollection = sourceSet.getRuntimeClasspath
