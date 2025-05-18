package org.podval.tools.build.scalajs

class ModuleInitializer(
  val moduleId: String,
  val className: String,
  val mainMethodName: Option[String],
  val mainMethodHasArgs: Boolean
)
