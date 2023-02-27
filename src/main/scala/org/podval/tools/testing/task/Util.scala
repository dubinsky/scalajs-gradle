package org.podval.tools.testing.task

import org.gradle.api.Task
import org.opentorah.build.Gradle.*
import scala.jdk.CollectionConverters.*

object Util:
  // TODO [classpath] add note with the reason wherever this is called
  // TODO [classpath] add configuration(s) to the build environment classpath with configuration.extendsFrom()
  // and remove addToClassPath; but first - look at the way zinc is used in the Scala plugin.
  def addConfigurationToClassPath(task: Task, configurationName: String): Unit =
    addToClassPath(task, task.getProject.getConfiguration(configurationName).asScala)
