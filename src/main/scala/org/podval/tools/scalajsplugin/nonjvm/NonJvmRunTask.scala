package org.podval.tools.scalajsplugin.nonjvm

import org.gradle.api.GradleException
import org.podval.tools.scalajsplugin.{BackendTask, ScalaJSPlugin}

trait NonJvmRunTask[L <: NonJvmLinkTask[L]] extends NonJvmTask[L]:
  protected def linkTaskClass: Class[? <: L]

  final override protected def linkTask: L = ScalaJSPlugin
    .findDependsOnProviderOrTask(this, linkTaskClass)
    .getOrElse(throw GradleException(s"Task $getName must depend on a task of type ${linkTaskClass.getName}!"))

object NonJvmRunTask:
  abstract class Main[L <: NonJvmLinkTask[L]] extends BackendTask.Run.Main with NonJvmRunTask[L]
  abstract class Test[L <: NonJvmLinkTask[L]] extends BackendTask.Run.Test with NonJvmRunTask[L]
