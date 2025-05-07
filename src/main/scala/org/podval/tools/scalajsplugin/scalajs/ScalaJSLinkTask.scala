package org.podval.tools.scalajsplugin.scalajs

import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Optional, OutputDirectory, OutputFile, TaskAction}
import org.podval.tools.scalajs.{ModuleInitializer, ModuleKind, ModuleSplitStyle, Optimization, ScalaJSLink}
import org.podval.tools.scalajsplugin.nonjvm.NonJvmLinkTask
import java.io.File

trait ScalaJSLinkTask extends NonJvmLinkTask[ScalaJSLinkTask] with ScalaJSTask:
  final override protected def buildSubDirectory: String = "scalajs"

  def moduleInitializers: Option[Seq[ModuleInitializer]]

  @Input @Optional def getPrettyPrint: Property[Boolean]

  @Input def getModuleKind: Property[String]
  ModuleKind.convention(getModuleKind)

  @Input def getModuleSplitStyle: Property[String]
  ModuleSplitStyle.convention(getModuleSplitStyle)

  @Input def getOptimization: Property[String]
  Optimization.convention(getOptimization)
  
  @OutputDirectory final def getJSDirectory: File = outputFile("js")
  @OutputFile final def getReportTextFile: File = outputFile("linking-report.txt")
  @OutputFile final def getReportBinFile : File = outputFile("linking-report.bin")

  @TaskAction final def execute(): Unit = ScalaJSLink(scalaJSCommon).link(
    reportTextFile = getReportTextFile,
    optimization = Optimization(getOptimization),
    moduleSplitStyle = ModuleSplitStyle(getModuleSplitStyle),
    moduleInitializers = moduleInitializers,
    prettyPrint = getPrettyPrint.getOrElse(false),
    runtimeClassPath = runtimeClassPath,
  )
