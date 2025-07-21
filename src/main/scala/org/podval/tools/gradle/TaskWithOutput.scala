package org.podval.tools.gradle

import org.gradle.StartParameter
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.logging.LogLevel
import org.gradle.api.{Project, Task}
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.podval.tools.platform.Output

trait TaskWithOutput extends Task:
  @Internal def getLogLevelEnabled: Property[LogLevel]
  @Internal def getRunningInIntelliJ: Property[Boolean]

  final protected def output: Output = Output(
    logLevelEnabled = getLogLevelEnabled.get,
    isRunningInIntelliJ = getRunningInIntelliJ.get,
    logSource = getName
  )

object TaskWithOutput:
  def configureTasks(
    project: Project,
    isRunningInIntelliJ: Boolean
  ): Unit = Tasks.configureEach(
    project,
    classOf[TaskWithOutput],
    (task: TaskWithOutput) =>
      task.getLogLevelEnabled.set(project.asInstanceOf[ProjectInternal].getServices.get(classOf[StartParameter]).getLogLevel)
      task.getRunningInIntelliJ.set(isRunningInIntelliJ)
  )
