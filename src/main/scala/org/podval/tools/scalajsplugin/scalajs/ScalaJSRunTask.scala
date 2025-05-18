package org.podval.tools.scalajsplugin.scalajs

import org.podval.tools.node.Node
import org.podval.tools.scalajsplugin.nonjvm.NonJvmRunTask

trait ScalaJSRunTask extends NonJvmRunTask[ScalaJSLinkTask] with ScalaJSTask