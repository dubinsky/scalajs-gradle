package org.podval.tools.scalajs.js

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, InputFiles, Nested, Optional, OutputDirectory, OutputFile, SourceSet, TaskAction}
import org.gradle.api.{DefaultTask, NamedDomainObjectContainer}
import org.podval.tools.build.Gradle
import org.podval.tools.util.Files
import java.io.File
import scala.jdk.CollectionConverters.SetHasAsScala

sealed abstract class LinkTask extends DefaultTask with ScalaJSTask:
  setGroup("build")
  setDescription(s"$flavour ScalaJS${optimization.description}")
  getDependsOn.add(Gradle.getClassesTask(getProject, sourceSetName))

  def sourceSetName: String

  @TaskAction final def execute(): Unit = scalaJS.link()

  final override protected def linkTask: LinkTask = this

  def moduleInitializers: Option[Seq[ModuleInitializer]]

  @InputFiles def getRuntimeClassPath: ConfigurableFileCollection
  
  @Input @Optional def getPrettyPrint: Property[Boolean]
  final def prettyPrint: Boolean = 
    getPrettyPrint.getOrElse(false)
    
  @Input @Optional def getModuleKind: Property[String]
  final def moduleKind: ModuleKind = 
    LinkTask.byName(getModuleKind, ModuleKind.NoModule, ModuleKind.values.toList)
  
  @Input @Optional def getModuleSplitStyle: Property[String]
  final def moduleSplitStyle: ModuleSplitStyle = 
    LinkTask.byName(getModuleSplitStyle, ModuleSplitStyle.FewestModules, ModuleSplitStyle.values.toList)
  
  @Input @Optional def getOptimization: Property[String]
  final def optimization: Optimization = 
    LinkTask.byName(getOptimization, Optimization.Fast, Optimization.values.toList)

  private val buildDirectory: File = getProject.getLayout.getBuildDirectory.get.getAsFile
  private def outputFile(name: String): File = Files.file(buildDirectory, "scalajs", getName, name)
  @OutputDirectory final def getJSDirectory: File = outputFile("js")
  @OutputFile final def getReportTextFile: File = outputFile("linking-report.txt")
  @OutputFile final def getReportBinFile : File = outputFile("linking-report.bin")

object LinkTask:
  private def byName[T](property: Property[String], default: => T, all: List[T]): T =
    if !property.isPresent then default else all.find(_.toString == property.get).get

  abstract class Main extends LinkTask:
    final override protected def flavour: String = "Link"
    final override def sourceSetName: String = SourceSet.MAIN_SOURCE_SET_NAME
    @Nested def getModuleInitializers: NamedDomainObjectContainer[ModuleInitializerProperties]
    final override def moduleInitializers: Option[Seq[ModuleInitializer]] =
      Some(getModuleInitializers.asScala.toSeq.map(_.toModuleInitializer))

  abstract class Test extends LinkTask:
    final override protected def flavour: String = "LinkTest"
    final override def sourceSetName: String = SourceSet.TEST_SOURCE_SET_NAME
    final override def moduleInitializers: Option[Seq[ModuleInitializer]] =
      None
