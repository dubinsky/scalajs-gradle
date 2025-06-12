package org.podval.tools.backendplugin.scalanative

import org.podval.tools.node.Node
import org.podval.tools.backendplugin.nonjvm.NonJvmRunTask

trait ScalaNativeRunTask extends NonJvmRunTask[ScalaNativeLinkTask] with ScalaNativeTask
