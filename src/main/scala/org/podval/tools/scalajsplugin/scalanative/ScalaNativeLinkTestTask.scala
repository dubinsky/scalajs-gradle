package org.podval.tools.scalajsplugin.scalanative

import org.podval.tools.scalajsplugin.nonjvm.BackendLinkTestTask

abstract class ScalaNativeLinkTestTask extends BackendLinkTestTask[ScalaNativeLinkTask] with ScalaNativeLinkTask:
  override protected def mainClass: Option[String] = Some("scala.scalanative.testinterface.TestMain")
