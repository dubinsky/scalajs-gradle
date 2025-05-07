package org.podval.tools.scalajsplugin.scalanative

import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Optional}
import org.podval.tools.build.Gradle
import org.podval.tools.scalajsplugin.nonjvm.NonJvmLinkMainTask

abstract class ScalaNativeLinkMainTask extends NonJvmLinkMainTask[ScalaNativeLinkTask] with ScalaNativeLinkTask:
  @Input @Optional def getMainClass: Property[String]
  
  override protected def mainClass: Option[String] = Gradle.toOption(getMainClass)
