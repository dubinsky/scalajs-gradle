package org.podval.tools.scalajs

import org.gradle.api.provider.Property
import org.scalajs.linker.interface.ModuleInitializer

abstract class ModuleInitializerProperties:
  def getName: String // Type must have a read-only 'name' property
  def getClassName: Property[String]
  def getMainMethodName: Property[String]
  def getMainMethodHasArgs: Property[Boolean]

  final def toModuleInitializer: ModuleInitializer =
    val clazz : String = getClassName.get
    val method: String = getMainMethodName.getOrElse("main")
    // TODO use the name as the module id:
    if getMainMethodHasArgs.getOrElse(false)
    then ModuleInitializer.mainMethodWithArgs(clazz, method)//.withModuleID(getName)
    else ModuleInitializer.mainMethod        (clazz, method)//.withModuleID(getName)
