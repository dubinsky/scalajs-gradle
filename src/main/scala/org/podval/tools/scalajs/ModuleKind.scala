package org.podval.tools.scalajs

import org.podval.tools.build.Named

enum ModuleKind(name: String) extends Named(name) derives CanEqual:
  case NoModule       extends ModuleKind("NoModule")
  case ESModule       extends ModuleKind("ESModule")
  case CommonJSModule extends ModuleKind("CommonJSModule")
  
object ModuleKind extends Named.Companion[ModuleKind]("ModuleKind"):
  override def default: ModuleKind = NoModule
  override def all: Seq[ModuleKind] = values.toSeq
