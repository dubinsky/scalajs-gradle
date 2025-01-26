package org.podval.tools.build

import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet
import org.podval.tools.build.Gradle.*

trait TaskWithSourceSet extends Task:
  protected final def sourceSet: SourceSet = getProject.getSourceSet(sourceSetName)

  protected def sourceSetName: String
