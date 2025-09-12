package org.podval.tools.backend

import org.gradle.api.{Project, Task}
import org.podval.tools.build.ScalaBackend
import org.podval.tools.gradle.Tasks

final class SharedProject(
  project: Project,
  val backends: Set[ScalaBackend]
) extends SingleProject(
  project
):
  override def announcement: String = s"code shared between backends ${ScalaBackend.names(backends)}"

  override def apply(): Unit =
    // Disable all tasks.
    Tasks.disable(project, classOf[Task])

  override def afterEvaluate(): Unit =
    ()
    