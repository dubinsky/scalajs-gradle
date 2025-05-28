package org.podval.tools.scalaplugin.scalajs

import org.podval.tools.node.Node
import org.podval.tools.scalaplugin.nonjvm.NonJvmRunTask

trait ScalaJSRunTask extends NonJvmRunTask[ScalaJSLinkTask] with ScalaJSTask