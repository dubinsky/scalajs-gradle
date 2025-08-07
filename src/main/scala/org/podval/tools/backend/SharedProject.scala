package org.podval.tools.backend

import org.gradle.api.{Project, Task}
import org.podval.tools.build.ScalaBackend
import org.podval.tools.gradle.Tasks

final class SharedProject(
  project: Project,
  val sourceRoot: String,
  val backends: Set[ScalaBackend]
) extends SingleProject(
  project
):
  override def apply(): Unit =
    announce(s"code shared between backends ${backends.map(_.name).mkString(", ")}")

    setScalaVersionFromParentAndAddVersionSpecificSources()

    // Disable all tasks.
    Tasks.disable(project, classOf[Task])
