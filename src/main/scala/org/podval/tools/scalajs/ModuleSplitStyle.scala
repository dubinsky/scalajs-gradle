package org.podval.tools.scalajs

import org.podval.tools.build.Named

enum ModuleSplitStyle(name: String) extends Named(name) derives CanEqual:
  case FewestModules   extends ModuleSplitStyle("FewestModules")
  case SmallestModules extends ModuleSplitStyle("SmallestModules")
  case SmallModulesFor extends ModuleSplitStyle("SmallModulesFor")

object ModuleSplitStyle extends Named.Companion[ModuleSplitStyle]("ModuleSplitStyle"):
  override def default: ModuleSplitStyle = FewestModules
  override def all: Seq[ModuleSplitStyle] = values.toSeq
  