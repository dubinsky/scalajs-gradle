package org.podval.tools.scalajs

import org.gradle.api.{NamedDomainObjectContainer, Project}
import org.gradle.api.provider.Property
import org.opentorah.build.Gradle.*

abstract class Extension(val project: Project):
  def getModuleInitializers: NamedDomainObjectContainer[ModuleInitializerProperties]

  def getStage: Property[String]
  def stage: Stage = getStage.byName(Stage.FastOpt, Stage.all)

  def getModuleKind: Property[String]

  def getModuleSplitStyle: Property[String]

  def getPrettyPrint: Property[Boolean]
