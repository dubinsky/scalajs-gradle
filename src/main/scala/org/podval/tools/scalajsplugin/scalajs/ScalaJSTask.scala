package org.podval.tools.scalajsplugin.scalajs

import org.gradle.api.GradleException
import org.podval.tools.node.TaskWithNode
import org.podval.tools.scalajsplugin.nonjvm.NonJvmTask

trait ScalaJSTask extends NonJvmTask[ScalaJSLinkTask] with TaskWithNode:
  protected final def abort: String => Nothing = (message: String) => throw GradleException(message)
