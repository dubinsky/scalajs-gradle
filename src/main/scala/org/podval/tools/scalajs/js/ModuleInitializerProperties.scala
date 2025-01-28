package org.podval.tools.scalajs.js

import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Optional}

abstract class ModuleInitializerProperties:
  @Input def getName: String // Type must have a read-only 'name' property
  @Input def getClassName: Property[String]
  @Input @Optional def getMainMethodName: Property[String]
  @Input @Optional def getMainMethodHasArgs: Property[Boolean]
