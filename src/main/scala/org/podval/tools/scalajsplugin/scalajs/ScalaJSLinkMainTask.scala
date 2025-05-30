package org.podval.tools.scalajsplugin.scalajs

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.Nested
import org.podval.tools.build.scalajs.ModuleInitializer
import org.podval.tools.scalajsplugin.nonjvm.NonJvmLinkTask
import scala.jdk.CollectionConverters.SetHasAsScala

abstract class ScalaJSLinkMainTask extends NonJvmLinkTask.Main[ScalaJSLinkTask] with ScalaJSLinkTask:
  @Nested def getModuleInitializers: NamedDomainObjectContainer[ModuleInitializerProperties]

  final override def moduleInitializers: Option[Seq[ModuleInitializer]] =
    Some(getModuleInitializers.asScala.toSeq.map(_.toModuleInitializer))
