package org.podval.tools.scalajsplugin.scalanative

import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Optional}
import org.podval.tools.scalajsplugin.nonjvm.NonJvmLinkTask

abstract class ScalaNativeLinkMainTask extends NonJvmLinkTask.Main[ScalaNativeLinkTask] with ScalaNativeLinkTask:
  @Input @Optional def getMainClass: Property[String]
  
  override protected def mainClass: Option[String] = Option(getMainClass.getOrNull)
