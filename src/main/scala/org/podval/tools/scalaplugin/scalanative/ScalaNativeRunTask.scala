package org.podval.tools.scalaplugin.scalanative

import org.podval.tools.node.Node
import org.podval.tools.scalaplugin.nonjvm.NonJvmRunTask

trait ScalaNativeRunTask extends NonJvmRunTask[ScalaNativeLinkTask] with ScalaNativeTask
