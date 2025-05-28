package org.podval.tools.scalaplugin.scalajs

import org.podval.tools.node.TaskWithNode
import org.podval.tools.scalaplugin.nonjvm.NonJvmTask

trait ScalaJSTask extends NonJvmTask[ScalaJSLinkTask] with TaskWithNode
