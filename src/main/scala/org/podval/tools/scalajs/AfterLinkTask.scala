package org.podval.tools.scalajs

import org.gradle.api.Project

abstract class AfterLinkTask[T <: LinkTask](clazz: Class[T]) extends ScalaJSTask:

  // Find corresponding LinkTask and depend on it:
  final def linkTask: LinkTask =
    getProject.getTasks.withType(clazz).iterator.next

  getProject.afterEvaluate { (_: Project) =>
    getDependsOn.add(linkTask)
    () // return Unit to help the compiler find the correct overload
  }
