package org.podval.tools.scalajs

import org.scalajs.linker.interface.{ModuleKind, ModuleSplitStyle}

object Util:
  given CanEqual[ModuleKind, ModuleKind] = CanEqual.derived

  val moduleSplitStyles: List[ModuleSplitStyle] = List(ModuleSplitStyle.FewestModules, ModuleSplitStyle.SmallestModules)
