package org.podval.tools.scalajsplugin.scalajs

import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Optional}
import org.podval.tools.build.Gradle
import org.podval.tools.scalajs.ModuleInitializer

abstract class ModuleInitializerProperties:
  @Input def getName: String // Type must have a read-only 'name' property
  @Input def getClassName: Property[String]
  @Input @Optional def getMainMethodName: Property[String]
  @Input @Optional def getMainMethodHasArgs: Property[Boolean]

  final def toModuleInitializer: ModuleInitializer = ModuleInitializer(
    moduleId = getName,
    className = getClassName.get,
    mainMethodName = Gradle.toOption(getMainMethodName),
    mainMethodHasArgs = getMainMethodHasArgs.getOrElse(false)
  )
