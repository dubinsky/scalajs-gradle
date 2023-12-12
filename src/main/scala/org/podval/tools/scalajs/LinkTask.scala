package org.podval.tools.scalajs

import org.gradle.api.{DefaultTask, NamedDomainObjectContainer, Project}
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Classpath, Input, Nested, Optional, OutputDirectory, OutputFile, SourceSet, TaskAction}
import org.opentorah.build.Gradle.*
import org.opentorah.util.Files
import java.io.File
import scala.jdk.CollectionConverters.*

sealed abstract class LinkTask extends DefaultTask with ScalaJSTask:
  setGroup("build")

  @TaskAction final def execute(): Unit =
    // Needed to access ScalaJS linking functionality in Link.
    // Dynamically-loaded classes are can only be loaded after they are added to the classpath,
    // or Gradle decorating code breaks at the plugin load time for the Task subclasses.
    // So, dynamically-loaded classes are mentioned indirectly, only in the ScalaJS class.
    // It seems that expanding the classpath once, here, is enough for everything to work.
    addToClassPath(this, getProject.getConfiguration(ScalaJSDependencies.configurationName).asScala)

    ScalaJS(task = this, linkTask = this).link()

  protected def sourceSetName: String
  private def sourceSet: SourceSet = getProject.getSourceSet(sourceSetName)

  def moduleInitializerProperties: Option[Seq[ModuleInitializerProperties]]

  @Classpath final def getRuntimeClassPath: FileCollection = sourceSet.getRuntimeClasspath

  @OutputDirectory final def getJSDirectory: File = outputFile("js")
  @OutputFile final def getReportTextFile: File = outputFile("linking-report.txt")
  @OutputFile final def getReportBinFile : File = outputFile("linking-report.bin")
  private def outputFile(name: String): File = Files.file(getProject.getLayout.getBuildDirectory.get.getAsFile, "scalajs", getName, name)

  def optimization: Optimization = getOptimization.byName(Optimization.Fast, Optimization.values.toList)
  @Input @Optional def getOptimization    : Property[String]
  @Input @Optional def getModuleKind      : Property[String]
  @Input @Optional def getModuleSplitStyle: Property[String]
  @Input @Optional def getPrettyPrint     : Property[Boolean]

  getProject.afterEvaluate((project: Project) =>
    getDependsOn.add(project.getClassesTask(sourceSet))
    setDescription(s"$flavour ScalaJS${optimization.description}")
  )

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

