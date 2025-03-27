package org.podval.tools.scalajsplugin.scalajs

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.Nested
import org.podval.tools.scalajs.ModuleInitializer
import scala.jdk.CollectionConverters.SetHasAsScala

abstract class ScalaJSLinkMainTask extends ScalaJSLinkTask("Link"):
  @Nested def getModuleInitializers: NamedDomainObjectContainer[ModuleInitializerProperties]

  final override def moduleInitializers: Option[Seq[ModuleInitializer]] =
    Some(getModuleInitializers.asScala.toSeq.map(_.toModuleInitializer))
