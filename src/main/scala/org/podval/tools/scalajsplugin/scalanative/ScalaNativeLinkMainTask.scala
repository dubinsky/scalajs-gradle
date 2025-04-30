package org.podval.tools.scalajsplugin.scalanative

import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Optional}
import org.podval.tools.build.Gradle
import org.podval.tools.scalajsplugin.nonjvm.BackendLinkMainTask

abstract class ScalaNativeLinkMainTask extends BackendLinkMainTask[ScalaNativeLinkTask] with ScalaNativeLinkTask:
  override protected def mainClass: Option[String] = Gradle.toOption(getMainClass)

  @Input @Optional def getMainClass: Property[String]
