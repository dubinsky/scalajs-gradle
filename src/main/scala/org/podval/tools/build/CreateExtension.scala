package org.podval.tools.build

import org.gradle.api.Project

final class CreateExtension[T](
  name: String,
  clazz: Class[T]
):
  def apply(project: Project): T = project
    .getExtensions
    .create(
      name,
      clazz
    )
