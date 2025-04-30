package org.podval.tools.scalajsplugin.nonjvm

import org.gradle.api.GradleException
import scala.jdk.CollectionConverters.SetHasAsScala

trait BackendRunTask[L <: BackendLinkTask[L]] extends BackendTask[L]:
  // TODO assign when creating the tasks!
  protected def linkTaskClass: Class[? <: L]

  final override def flavourSuffix: String = linkTask.flavourSuffix

  // TODO when switching to TaskProviders, adjust this:
  final override protected def linkTask: L = getDependsOn
    .asScala
    .find((candidate: AnyRef) => linkTaskClass.isAssignableFrom(candidate.getClass))
    .map(_.asInstanceOf[L])
    .getOrElse(throw GradleException(s"Task $getName must depend on a task of type ${linkTaskClass.getName}!"))
