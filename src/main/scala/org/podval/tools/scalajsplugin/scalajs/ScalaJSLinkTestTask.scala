package org.podval.tools.scalajsplugin.scalajs

import org.gradle.api.tasks.SourceSet
import org.podval.tools.scalajs.ModuleInitializer

abstract class ScalaJSLinkTestTask extends ScalaJSLinkTask:
  final override protected def flavour: String = "LinkTest"

  final override def sourceSetName: String = SourceSet.TEST_SOURCE_SET_NAME

  final override def moduleInitializers: Option[Seq[ModuleInitializer]] = None
