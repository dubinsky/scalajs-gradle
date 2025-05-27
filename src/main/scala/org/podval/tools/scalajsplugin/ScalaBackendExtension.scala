package org.podval.tools.scalajsplugin

import org.gradle.api.Project
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ScalaBackendExtension  @Inject(project: Project):
  def getName: Property[String]

  def getVersion: Property[String]
