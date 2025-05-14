package org.podval.tools.scalajsplugin.scalanative

import org.podval.tools.scalajsplugin.nonjvm.NonJvmLinkTask

abstract class ScalaNativeLinkTestTask extends NonJvmLinkTask.Test[ScalaNativeLinkTask] with ScalaNativeLinkTask:
  override protected def mainClass: Option[String] = Some("scala.scalanative.testinterface.TestMain")
