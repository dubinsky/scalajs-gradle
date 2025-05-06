package org.podval.tools.scalajsplugin.scalanative

import org.podval.tools.scalajsplugin.nonjvm.NonJvmLinkTestTask

abstract class ScalaNativeLinkTestTask extends NonJvmLinkTestTask[ScalaNativeLinkTask] with ScalaNativeLinkTask:
  override protected def mainClass: Option[String] = Some("scala.scalanative.testinterface.TestMain")
