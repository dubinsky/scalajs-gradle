package org.podval.tools.backendplugin.scalajs

import org.podval.tools.backend.scalajs.ModuleInitializer
import org.podval.tools.backendplugin.nonjvm.NonJvmLinkTask

abstract class ScalaJSLinkTestTask extends NonJvmLinkTask.Test[ScalaJSLinkTask] with ScalaJSLinkTask:
  final override def moduleInitializers: Option[Seq[ModuleInitializer]] = None
