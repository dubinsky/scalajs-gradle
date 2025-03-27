package org.podval.tools.scalajsplugin.scalajs

import org.podval.tools.scalajs.ModuleInitializer

abstract class ScalaJSLinkTestTask extends ScalaJSLinkTask("LinkTest"):
  final override def moduleInitializers: Option[Seq[ModuleInitializer]] = None
