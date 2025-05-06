package org.podval.tools.scalajsplugin.scalajs

import org.podval.tools.scalajs.ModuleInitializer
import org.podval.tools.scalajsplugin.nonjvm.NonJvmLinkTestTask

abstract class ScalaJSLinkTestTask extends NonJvmLinkTestTask[ScalaJSLinkTask] with ScalaJSLinkTask:
  final override def moduleInitializers: Option[Seq[ModuleInitializer]] = None
