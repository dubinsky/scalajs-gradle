package org.podval.tools.scalajs

import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Optional}

abstract class ModuleInitializerProperties:
  @Input def getName: String // Type must have a read-only 'name' property
  @Input def getClassName: Property[String]
  @Input @Optional def getMainMethodName: Property[String]
  @Input @Optional def getMainMethodHasArgs: Property[Boolean]

  final def toModuleInitializer: ModuleInitializer = ModuleInitializer(
    moduleId = getName,
    className = getClassName.get,
    mainMethodName = Option(getMainMethodName.getOrNull),
    mainMethodHasArgs = getMainMethodHasArgs.getOrElse(false)
  )
