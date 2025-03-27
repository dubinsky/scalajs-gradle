package org.podval.tools.scalajsplugin.scalajs

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, InputFiles, Optional, OutputDirectory, OutputFile, TaskAction}
import org.gradle.api.DefaultTask
import org.podval.tools.scalajs.{ModuleInitializer, ModuleKind, ModuleSplitStyle, Optimization, ScalaJSLink}
import org.podval.tools.util.Files
import java.io.File
import scala.jdk.CollectionConverters.SetHasAsScala

abstract class ScalaJSLinkTask(
  final override protected val flavour: String
) extends DefaultTask with ScalaJSTask:
  setGroup("build")
  setDescription(s"$flavour ScalaJS${optimization.description}")
  
  @TaskAction final def execute(): Unit = ScalaJSLink(scalaJSCommon).link(
    reportTextFile = linkTask.getReportTextFile,
    optimization = optimization,
    moduleSplitStyle = moduleSplitStyle,
    moduleInitializers = moduleInitializers,
    prettyPrint = prettyPrint,
    runtimeClassPath = getRuntimeClassPath.getFiles.asScala.toSeq,
  )

  final override protected def linkTask: ScalaJSLinkTask = this

  def moduleInitializers: Option[Seq[ModuleInitializer]]

  @InputFiles def getRuntimeClassPath: ConfigurableFileCollection

  @Input @Optional def getPrettyPrint: Property[Boolean]
  final def prettyPrint: Boolean =
    getPrettyPrint.getOrElse(false)

  @Input @Optional def getModuleKind: Property[String]
  final def moduleKind: ModuleKind =
    ScalaJSLinkTask.byName(getModuleKind, ModuleKind.NoModule, ModuleKind.values.toList)

  @Input @Optional def getModuleSplitStyle: Property[String]
  final def moduleSplitStyle: ModuleSplitStyle =
    ScalaJSLinkTask.byName(getModuleSplitStyle, ModuleSplitStyle.FewestModules, ModuleSplitStyle.values.toList)

  @Input @Optional def getOptimization: Property[String]
  final def optimization: Optimization =
    ScalaJSLinkTask.byName(getOptimization, Optimization.Fast, Optimization.values.toList)

  private val buildDirectory: File = getProject.getLayout.getBuildDirectory.get.getAsFile
  private def outputFile(name: String): File = Files.file(buildDirectory, "scalajs", getName, name)
  @OutputDirectory final def getJSDirectory: File = outputFile("js")
  @OutputFile final def getReportTextFile: File = outputFile("linking-report.txt")
  @OutputFile final def getReportBinFile : File = outputFile("linking-report.bin")

object ScalaJSLinkTask:
  private def byName[T](property: Property[String], default: => T, all: List[T]): T =
    if !property.isPresent then default else all.find(_.toString == property.get).get
