package org.podval.tools.scalajs

import org.gradle.api.GradleException
import scala.jdk.CollectionConverters.*

trait AfterLink extends ScalaJSTask:
  protected def linkTaskClass: Class[? <: Link]
  
  final def linkTask: Link = getDependsOn
    .asScala
    .find(candidate => linkTaskClass.isAssignableFrom(candidate.getClass))
    .map(_.asInstanceOf[Link])
    .getOrElse(throw new GradleException(s"Task $getName must depend on a task of type ${linkTaskClass.getName}!"))
