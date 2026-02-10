package org.podval.tools.build

import org.gradle.StartParameter
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.{Project, Task}
import org.podval.tools.util.Tasks

trait OutputTask extends Task:
  @Internal def getLogLevelEnabled: Property[LogLevel]
  @Internal def getRunningInIntelliJ: Property[Boolean]

  final protected def output: Output = Output(
    logLevelEnabled = getLogLevelEnabled.get,
    isRunningInIntelliJ = getRunningInIntelliJ.get,
    logSource = getName
  )

object OutputTask:
  def configureTasks(
    project: Project,
    isRunningInIntelliJ: Boolean
  ): Unit =
    val logLevel: LogLevel = project
      .asInstanceOf[ProjectInternal]
      .getServices
      .get(classOf[StartParameter])
      .getLogLevel
    
    Tasks.configureEach(
      project,
      classOf[OutputTask],
      (task: OutputTask) =>
        task.getLogLevelEnabled.set(logLevel)
        task.getRunningInIntelliJ.set(isRunningInIntelliJ)
    )
  
