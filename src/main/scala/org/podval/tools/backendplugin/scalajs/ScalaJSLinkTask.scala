package org.podval.tools.backendplugin.scalajs

import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Optional, OutputDirectory, OutputFile, TaskAction}
import org.podval.tools.backend.scalajs.{ExperimentalUseWebAssembly, ModuleInitializer, ModuleKind, ModuleSplitStyle, 
  Optimization, ScalaJSBuild}
import org.podval.tools.backendplugin.nonjvm.NonJvmLinkTask
import org.podval.tools.util.Named
import java.io.File

trait ScalaJSLinkTask extends NonJvmLinkTask[ScalaJSLinkTask] with ScalaJSTask:
  def moduleInitializers: Option[Seq[ModuleInitializer]]

  @Input @Optional def getPrettyPrint: Property[Boolean]

  @Input def getModuleKind: Property[String]
  ModuleKind.convention(getModuleKind)
  def moduleKind: ModuleKind = ModuleKind(getModuleKind)

  @Input def getModuleSplitStyle: Property[String]
  ModuleSplitStyle.convention(getModuleSplitStyle)

  @Input def getOptimization: Property[String]
  Optimization.convention(getOptimization)
  
  @Input def getExperimentalUseWebAssembly: Property[Boolean]
  ExperimentalUseWebAssembly.convention(getExperimentalUseWebAssembly)
  def experimentalUseWebAssembly: Boolean = ExperimentalUseWebAssembly(getExperimentalUseWebAssembly)
  
  @OutputDirectory final def getJSDirectory: File = outputFile("js")
  @OutputFile final def getReportTextFile: File = outputFile("linking-report.txt")
  @OutputFile final def getReportBinFile : File = outputFile("linking-report.bin")

  @TaskAction final def execute(): Unit = ScalaJSBuild.link(
    jsDirectory = getJSDirectory,
    reportBinFile = getReportBinFile,
    reportTextFile = getReportTextFile,
    optimization = Optimization(getOptimization),
    moduleKind = moduleKind,
    moduleSplitStyle = ModuleSplitStyle(getModuleSplitStyle),
    useWebAssembly = experimentalUseWebAssembly,
    moduleInitializers = moduleInitializers,
    prettyPrint = getPrettyPrint.getOrElse(false),
    runtimeClassPath = runtimeClassPath,
    logSource = getName,
    abort = abort
  )
