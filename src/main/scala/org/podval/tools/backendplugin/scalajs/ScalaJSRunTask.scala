package org.podval.tools.backendplugin.scalajs

import org.podval.tools.node.Node
import org.podval.tools.backendplugin.nonjvm.NonJvmRunTask

trait ScalaJSRunTask extends NonJvmRunTask[ScalaJSLinkTask] with ScalaJSTask