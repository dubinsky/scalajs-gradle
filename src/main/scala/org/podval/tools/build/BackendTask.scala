package org.podval.tools.build

import org.gradle.api.Task

trait BackendTask[B <: ScalaBackend] extends Task
