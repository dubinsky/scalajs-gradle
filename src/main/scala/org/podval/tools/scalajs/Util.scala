package org.podval.tools.scalajs

import org.scalajs.linker.interface.{ModuleKind, ModuleSplitStyle}

object Util:
  given CanEqual[ModuleKind, ModuleKind] = CanEqual.derived

  val moduleSplitStyles: List[ModuleSplitStyle] = List(ModuleSplitStyle.FewestModules, ModuleSplitStyle.SmallestModules)

  // Report:
  // IO.write(reportFile, Report.serialize(report))
  // Report.deserialize(IO.readBytes(reportFile))

  // Compiler options:
  //   if (scalaVersion.value.startsWith("2.")) {
  //    if (!scalacOpts.exists(opt => opt.startsWith("-Xplugin:") && opt.contains("scalajs-compiler")))
  //      warnMissingScalacOption("The `scalajs-compiler.jar` compiler plugin")
  //  } else {
  //    if (!scalacOpts.contains("-scalajs"))
  //      warnMissingScalacOption("The `-scalajs` flag")
  //  }
