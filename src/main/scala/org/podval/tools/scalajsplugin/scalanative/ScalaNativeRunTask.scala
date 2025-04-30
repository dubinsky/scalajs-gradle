package org.podval.tools.scalajsplugin.scalanative

import org.podval.tools.node.Node
import org.podval.tools.scalajs.ScalaJSRunCommon
import org.podval.tools.scalajsplugin.nonjvm.BackendRunTask

trait ScalaNativeRunTask extends BackendRunTask[ScalaNativeLinkTask] with ScalaNativeTask
