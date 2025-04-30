package org.podval.tools.scalajsplugin.scalajs

import org.podval.tools.scalajs.ModuleInitializer
import org.podval.tools.scalajsplugin.nonjvm.BackendLinkTestTask

abstract class ScalaJSLinkTestTask extends BackendLinkTestTask[ScalaJSLinkTask] with ScalaJSLinkTask:
  final override def moduleInitializers: Option[Seq[ModuleInitializer]] = None
