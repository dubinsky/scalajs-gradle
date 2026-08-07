package org.podval.tools.backend

import org.gradle.api.{Project, Task}
import org.podval.tools.build.Backend
import org.podval.tools.util.{Strings, Tasks}

final class SharedProject(
  project: Project,
  val backends: Set[Backend]
) extends SingleProject(
  project
):
  override def announcement: String = s"code shared between backends ${Strings.toString(backends, _.name)}"

  override def apply(): Unit =
    // Written by Grok ;)
    // Shared projects only contribute sources to backend siblings; they must not build, test, or
    // package. Keep Help-group tasks enabled so plugins such as ben-manes' "versions" can still run
    // partialDependencyUpdates (disabled producers leave missing partial JSON and break the
    // aggregate dependencyUpdates task).
    Tasks.configureEach(
      project,
      classOf[Task],
      (task: Task) =>
        if !Option(task.getGroup).exists(_.equalsIgnoreCase("help")) then
          task.setEnabled(false)
    )

  override def afterEvaluate(): Unit =
    ()
