package org.podval.tools.scalajsplugin.scalanative

import org.podval.tools.scalajsplugin.nonjvm.BackendTask

trait ScalaNativeTask extends BackendTask[ScalaNativeLinkTask]:
  final override protected def backend: String = "Scala Native"
