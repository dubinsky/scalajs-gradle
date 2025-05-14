package org.podval.tools.build

import org.gradle.api.Project

final class CreateExtension[T](
  name: String,
  clazz: Class[T],
  configure: T => Unit
):
  def create(project: Project): T = 
    val extension: T = project.getExtensions.create(name, clazz)
    configure(extension)
    extension
