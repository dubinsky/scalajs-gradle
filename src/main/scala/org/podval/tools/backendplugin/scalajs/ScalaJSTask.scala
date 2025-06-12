package org.podval.tools.backendplugin.scalajs

import org.podval.tools.node.TaskWithNode
import org.podval.tools.backendplugin.nonjvm.NonJvmTask

trait ScalaJSTask extends NonJvmTask[ScalaJSLinkTask] with TaskWithNode
