package org.podval.tools.scalajsplugin.scalajs

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.{Nested, SourceSet}
import org.podval.tools.scalajs.ModuleInitializer
import scala.jdk.CollectionConverters.SetHasAsScala

abstract class ScalaJSLinkMainTask extends ScalaJSLinkTask:
  final override protected def flavour: String = "Link"

  final override def sourceSetName: String = SourceSet.MAIN_SOURCE_SET_NAME

  @Nested def getModuleInitializers: NamedDomainObjectContainer[ModuleInitializerProperties]

  final override def moduleInitializers: Option[Seq[ModuleInitializer]] =
    Some(getModuleInitializers.asScala.toSeq.map(_.toModuleInitializer))
