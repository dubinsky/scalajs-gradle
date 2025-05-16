package org.podval.tools.scalajsplugin.scalanative

import org.podval.tools.node.Node
import org.podval.tools.scalajsplugin.nonjvm.NonJvmRunTask

trait ScalaNativeRunTask extends NonJvmRunTask[ScalaNativeLinkTask] with ScalaNativeTask
