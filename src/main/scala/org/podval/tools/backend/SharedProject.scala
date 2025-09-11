package org.podval.tools.backend

import org.gradle.api.{Project, Task}
import org.podval.tools.build.ScalaBackend
import org.podval.tools.gradle.{Projects, Tasks}

final class SharedProject(
  project: Project,
  val backends: Set[ScalaBackend]
) extends SingleProject(
  project
):
  override def apply(): Unit =
    announce(s"code shared between backends ${ScalaBackend.names(backends)}")

    // Disable all tasks.
    Tasks.disable(project, classOf[Task])

    // Set Scala version from parent.
    setScalaVersionFromParent(Projects.parent(project).get)

    // Add version-specific sources.
    project.afterEvaluate((_: Project) => addVersionSpecificSources())
