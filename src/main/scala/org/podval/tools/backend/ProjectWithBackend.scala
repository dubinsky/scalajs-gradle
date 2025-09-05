package org.podval.tools.backend

import org.gradle.api.Project
import org.podval.tools.build.ScalaBackend

final class ProjectWithBackend(
  project: Project,
  val backend: ScalaBackend
) extends WithProject(
  project
)
