package org.podval.tools.scalajsplugin.nonjvm

import org.gradle.api.GradleException
import org.podval.tools.build.Gradle

trait NonJvmRunTask[L <: NonJvmLinkTask[L]] extends NonJvmTask[L]:
  protected def linkTaskClass: Class[? <: L]

  final override protected def linkTask: L = Gradle
    .findDependsOnProviderOrTask(this, linkTaskClass)
    .getOrElse(throw GradleException(s"Task $getName must depend on a task of type ${linkTaskClass.getName}!"))
 