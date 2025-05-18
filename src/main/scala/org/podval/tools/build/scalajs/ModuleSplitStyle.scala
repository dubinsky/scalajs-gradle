package org.podval.tools.build.scalajs

import org.podval.tools.util.Named

enum ModuleSplitStyle(name: String) extends Named(name) derives CanEqual:
  case FewestModules   extends ModuleSplitStyle("FewestModules")
  case SmallestModules extends ModuleSplitStyle("SmallestModules")

object ModuleSplitStyle extends Named.Companion[ModuleSplitStyle]("ModuleSplitStyle"):
  override def default: ModuleSplitStyle = FewestModules
  override def all: Seq[ModuleSplitStyle] = values.toSeq
  