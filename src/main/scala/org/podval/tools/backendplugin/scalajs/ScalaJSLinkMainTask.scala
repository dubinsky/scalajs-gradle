package org.podval.tools.backendplugin.scalajs

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.tasks.Nested
import org.podval.tools.backend.scalajs.ModuleInitializer
import org.podval.tools.backendplugin.nonjvm.NonJvmLinkTask
import scala.jdk.CollectionConverters.SetHasAsScala

abstract class ScalaJSLinkMainTask extends NonJvmLinkTask.Main[ScalaJSLinkTask] with ScalaJSLinkTask:
  @Nested def getModuleInitializers: NamedDomainObjectContainer[ModuleInitializerProperties]

  final override def moduleInitializers: Option[Seq[ModuleInitializer]] =
    Some(getModuleInitializers.asScala.toSeq.map(_.toModuleInitializer))
