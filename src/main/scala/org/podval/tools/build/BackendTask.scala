package org.podval.tools.build

import org.gradle.api.tasks.Internal
import org.gradle.api.Task

trait BackendTask extends Task:
  @Internal def isTest: Boolean

object BackendTask:
  trait Main extends BackendTask:
    override def isTest: Boolean = false

  trait Test extends BackendTask:
    override def isTest: Boolean = true
