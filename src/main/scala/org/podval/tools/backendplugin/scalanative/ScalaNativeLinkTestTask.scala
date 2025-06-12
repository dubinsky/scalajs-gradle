package org.podval.tools.backendplugin.scalanative

import org.podval.tools.backendplugin.nonjvm.NonJvmLinkTask

abstract class ScalaNativeLinkTestTask extends NonJvmLinkTask.Test[ScalaNativeLinkTask] with ScalaNativeLinkTask:
  override protected def mainClass: Option[String] = Some("scala.scalanative.testinterface.TestMain")
