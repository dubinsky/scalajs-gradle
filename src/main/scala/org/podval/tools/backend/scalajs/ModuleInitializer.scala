package org.podval.tools.backend.scalajs

class ModuleInitializer(
  val moduleId: String,
  val className: String,
  val mainMethodName: Option[String],
  val mainMethodHasArgs: Boolean
)
