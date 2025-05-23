package org.podval.tools.scalajsplugin.scalajs

import org.podval.tools.node.TaskWithNode
import org.podval.tools.scalajsplugin.nonjvm.NonJvmTask

trait ScalaJSTask extends NonJvmTask[ScalaJSLinkTask] with TaskWithNode
