package org.podval.tools.backend

import org.gradle.api.Project
import org.podval.tools.build.ScalaBackend

final class ProjectWithBackend(
  val project: Project,
  val backend: ScalaBackend
)
