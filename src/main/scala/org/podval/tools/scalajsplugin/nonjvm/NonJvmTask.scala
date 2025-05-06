package org.podval.tools.scalajsplugin.nonjvm

import groovy.lang.Closure
import org.gradle.api.{Project, Task}

trait NonJvmTask[L <: NonJvmLinkTask[L]] extends Task:
  override abstract def configure(configureClosure: Closure[?]): Task =
    // TODO set description for TaskProviders?
    val result: Task = super.configure(configureClosure)
    setDescription(s"$flavourBase $backend - $flavourSuffix.")
    result

  protected def backend: String
  
  protected def flavourBase: String

  def flavourSuffix: String

  protected def linkTask: L
