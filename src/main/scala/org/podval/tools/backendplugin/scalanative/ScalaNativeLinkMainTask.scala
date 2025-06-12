package org.podval.tools.backendplugin.scalanative

import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Optional}
import org.podval.tools.backendplugin.nonjvm.NonJvmLinkTask

abstract class ScalaNativeLinkMainTask extends NonJvmLinkTask.Main[ScalaNativeLinkTask] with ScalaNativeLinkTask:
  @Input @Optional def getMainClass: Property[String]
  
  override protected def mainClass: Option[String] = Option(getMainClass.getOrNull)
