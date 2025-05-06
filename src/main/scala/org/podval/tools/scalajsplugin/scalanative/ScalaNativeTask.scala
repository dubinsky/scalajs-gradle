package org.podval.tools.scalajsplugin.scalanative

import org.podval.tools.scalajsplugin.nonjvm.NonJvmTask

trait ScalaNativeTask extends NonJvmTask[ScalaNativeLinkTask]:
  final override protected def backend: String = "Scala Native"
