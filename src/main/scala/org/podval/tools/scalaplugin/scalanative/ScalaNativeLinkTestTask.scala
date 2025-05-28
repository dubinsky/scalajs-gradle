package org.podval.tools.scalaplugin.scalanative

import org.podval.tools.scalaplugin.nonjvm.NonJvmLinkTask

abstract class ScalaNativeLinkTestTask extends NonJvmLinkTask.Test[ScalaNativeLinkTask] with ScalaNativeLinkTask:
  override protected def mainClass: Option[String] = Some("scala.scalanative.testinterface.TestMain")
