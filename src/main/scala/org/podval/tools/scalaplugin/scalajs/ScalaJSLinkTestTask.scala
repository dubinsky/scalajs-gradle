package org.podval.tools.scalaplugin.scalajs

import org.podval.tools.build.scalajs.ModuleInitializer
import org.podval.tools.scalaplugin.nonjvm.NonJvmLinkTask

abstract class ScalaJSLinkTestTask extends NonJvmLinkTask.Test[ScalaJSLinkTask] with ScalaJSLinkTask:
  final override def moduleInitializers: Option[Seq[ModuleInitializer]] = None
