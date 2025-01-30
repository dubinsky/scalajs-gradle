package org.podval.tools.scalajs

class ModuleInitializer(
  val moduleId: String,
  val className: String,
  val mainMethodName: Option[String],
  val mainMethodHasArgs: Boolean
)
