package org.podval.tools.scalajs

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.{ListProperty, Property}
import org.gradle.api.tasks.{Input, Nested, Optional, OutputDirectory, OutputFile, TaskAction}
import org.podval.tools.build.LinkTask
import java.io.File
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava, SetHasAsScala}

trait ScalaJSLinkTask extends LinkTask:
  def moduleInitializers: Option[Seq[ModuleInitializer]]

  @Input @Optional def getPrettyPrint: Property[Boolean]

  @Input def getModuleKind: Property[String]
  ModuleKind.convention(getModuleKind)

  @Input def getModuleSplitStyle: Property[String]
  ModuleSplitStyle.convention(getModuleSplitStyle)

  @Input def getSmallModulesFor: ListProperty[String]
  getSmallModulesFor.convention(List.empty.asJava)

  @Input def getOptimization: Property[String]
  Optimization.convention(getOptimization)
  
  @Input def getExperimentalUseWebAssembly: Property[Boolean]
  ExperimentalUseWebAssembly.convention(getExperimentalUseWebAssembly)
  
  @OutputDirectory final def getJSDirectory: File = outputFile("js")
  @OutputFile final def getReportTextFile: File = outputFile("linking-report.txt")
  @OutputFile final def getReportBinFile : File = outputFile("linking-report.bin")

  final def link: ScalaJSLink = ScalaJSLink(
    jsDirectory = getJSDirectory,
    reportBinFile = getReportBinFile,
    moduleKind = ModuleKind(getModuleKind),
    useWebAssembly = ExperimentalUseWebAssembly(getExperimentalUseWebAssembly),
    logSource = getName
  )
  
  @TaskAction final def execute(): Unit = link.link(
    reportTextFile = getReportTextFile,
    optimization = Optimization(getOptimization),
    moduleSplitStyle = ModuleSplitStyle(getModuleSplitStyle),
    smallModulesFor = getSmallModulesFor.get.asScala.toList,
    moduleInitializers = moduleInitializers,
    prettyPrint = getPrettyPrint.getOrElse(false),
    runtimeClassPath = runtimeClassPath
  )

object ScalaJSLinkTask:
  abstract class Main extends LinkTask.Main with ScalaJSLinkTask:
    @Nested def getModuleInitializers: NamedDomainObjectContainer[ModuleInitializerProperties]
    final override def moduleInitializers: Option[Seq[ModuleInitializer]] =
      Some(getModuleInitializers.asScala.toSeq.map(_.toModuleInitializer))

  abstract class Test extends LinkTask.Test with ScalaJSLinkTask:
    final override def moduleInitializers: Option[Seq[ModuleInitializer]] = None
