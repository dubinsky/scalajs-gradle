package org.podval.tools.scalajs

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.{DefaultTask, NamedDomainObjectContainer}
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Classpath, Input, Nested, Optional, OutputDirectory, OutputFile, SourceSet, TaskAction}
import org.podval.tools.build.TaskWithSourceSet
import org.podval.tools.build.Gradle.*
import org.podval.tools.util.Files
import java.io.File
import scala.jdk.CollectionConverters.SetHasAsScala

sealed abstract class LinkTask extends DefaultTask with ScalaJSTask with TaskWithSourceSet:
  // To avoid invoking Task.getProject at execution time, some things are captured at creation:
  setGroup("build")
  setDescription(s"$flavour ScalaJS${optimization.description}")
  getDependsOn.add(getProject.getClassesTask(sourceSet))

  final override protected def linkTask: LinkTask = this

  private val buildDirectory: File = getProject.getLayout.getBuildDirectory.get.getAsFile
  
  // Note: configured by the plugin on all `LinkTask` to eliminate `Task.getProject` call during execution;
  // at task creation runtime classpath does not yet have Scala.js and other dependencies that the plugin adds. 
  @Classpath def getRuntimeClassPath: ConfigurableFileCollection
  
  def moduleInitializerProperties: Option[Seq[ModuleInitializerProperties]]

  @OutputDirectory final def getJSDirectory: File = outputFile("js")
  @OutputFile final def getReportTextFile: File = outputFile("linking-report.txt")
  @OutputFile final def getReportBinFile : File = outputFile("linking-report.bin")
  private def outputFile(name: String): File = Files.file(buildDirectory, "scalajs", getName, name)

  def optimization: Optimization = getOptimization.byName(Optimization.Fast, Optimization.values.toList)
  @Input @Optional def getOptimization    : Property[String]
  @Input @Optional def getModuleKind      : Property[String]
  @Input @Optional def getModuleSplitStyle: Property[String]
  @Input @Optional def getPrettyPrint     : Property[Boolean]

  @TaskAction final def execute(): Unit =
    scalaJS.link()

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

