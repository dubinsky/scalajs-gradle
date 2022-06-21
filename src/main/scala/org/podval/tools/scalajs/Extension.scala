package org.podval.tools.scalajs

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Property
import org.scalajs.linker.interface.{ModuleInitializer, ModuleKind}
import org.opentorah.build.Gradle.*
import scala.jdk.CollectionConverters.*

abstract class Extension:
  def getModuleInitializers: NamedDomainObjectContainer[ModuleInitializerProperties]
  // TODO unify with toSet() used by the DocBook plugin
  def moduleInitializers: Seq[ModuleInitializer] = getModuleInitializers
    .asScala
    .toSeq
    .map(_.toModuleInitializer)

  def getStage: Property[String]
  def stage: Stage = getStage.byName(Stage.FastOpt, Stage.all)

  def getModuleKind: Property[String]
  def moduleKind: ModuleKind = getModuleKind.byName(ModuleKind.NoModule, ModuleKind.All)

  def getModuleSplitStyle: Property[String]
  
  def getPrettyPrint: Property[Boolean]
