package org.podval.tools.scalajs

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.{DefaultTask, NamedDomainObjectContainer}
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, InputDirectory, InputFiles, Nested, Optional, OutputDirectory, OutputFile, SourceSet,
  TaskAction}
import org.podval.tools.build.TaskWithSourceSet
import org.podval.tools.build.Gradle.*
import org.podval.tools.util.Files
import java.io.File
import scala.jdk.CollectionConverters.SetHasAsScala

sealed abstract class LinkTask extends DefaultTask with ScalaJSTask with TaskWithSourceSet:
  setGroup("build")
  setDescription(s"$flavour ScalaJS${optimization.description}")
  getDependsOn.add(getProject.getClassesTask(sourceSet))

  @TaskAction final def execute(): Unit = scalaJS.link()

  final override protected def linkTask: LinkTask = this
  
  def moduleInitializerProperties: Option[Seq[ModuleInitializerProperties]]
  
  @InputFiles def getRuntimeClassPath: ConfigurableFileCollection
  @Input @Optional def getModuleKind      : Property[String]
  @Input @Optional def getModuleSplitStyle: Property[String]
  @Input @Optional def getPrettyPrint     : Property[Boolean]
  @Input @Optional def getOptimization    : Property[String]
  
  final def optimization: Optimization = getOptimization.byName(Optimization.Fast, Optimization.values.toList)

  @InputDirectory private val buildDirectory: File = getProject.getLayout.getBuildDirectory.get.getAsFile
  private def outputFile(name: String): File = Files.file(buildDirectory, "scalajs", getName, name)
  @OutputDirectory final def getJSDirectory: File = outputFile("js")
  @OutputFile final def getReportTextFile: File = outputFile("linking-report.txt")
  @OutputFile final def getReportBinFile : File = outputFile("linking-report.bin")

object LinkTask:
  abstract class Main extends LinkTask:
    final override protected def flavour: String = "Link"
    final override protected def sourceSetName: String = SourceSet.MAIN_SOURCE_SET_NAME
    @Nested def getModuleInitializers: NamedDomainObjectContainer[ModuleInitializerProperties]
    final override def moduleInitializerProperties: Option[Seq[ModuleInitializerProperties]] =
      Some(getModuleInitializers.asScala.toSeq)

  abstract class Test extends LinkTask:
    final override protected def flavour: String = "LinkTest"
    final override protected def sourceSetName: String = SourceSet.TEST_SOURCE_SET_NAME
    final override def moduleInitializerProperties: Option[Seq[ModuleInitializerProperties]] =
      None

