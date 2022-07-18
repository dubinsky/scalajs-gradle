package org.podval.tools.scalajs

import org.gradle.api.provider.Property

abstract class ModuleInitializerProperties:
  def getName: String // Type must have a read-only 'name' property
  def getClassName: Property[String]
  def getMainMethodName: Property[String]
  def getMainMethodHasArgs: Property[Boolean]
