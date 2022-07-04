package org.podval.tools.scalajs

import org.gradle.api.{DefaultTask, NamedDomainObjectContainer, Project}
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Classpath, Input, Optional, OutputDirectory, OutputFile, SourceSet, TaskAction}
import org.opentorah.build.Gradle.*
import org.opentorah.util.Files
import java.io.File

sealed abstract class Link extends DefaultTask with ScalaJSTask:
  setGroup("build")

  @TaskAction final def execute(): Unit = act(_.link())

  protected def sourceSetName: String

  private def sourceSet: SourceSet = getProject.getSourceSet(sourceSetName)

  private def outputFile(name: String): File = Files.file(
    getProject.getBuildDir,
    "scalajs",
    getName,
    name
  )

  @Classpath final def getRuntimeClassPath: FileCollection = sourceSet.getRuntimeClasspath
  @OutputDirectory final def getJSDirectory: File = outputFile("js")
  @OutputFile final def getReportTextFile: File = outputFile("linking-report.txt")
  @OutputFile final def getReportBinFile : File = outputFile("linking-report.bin")

  getProject.afterEvaluate((project: Project) =>
    getDependsOn.add(project.getClassesTask(sourceSet))
    setDescription(s"$flavour ScalaJS${optimization.description}")
  )

  final def optimization: Link.Optimization = getOptimization.byName(Link.Optimization.Fast, Link.Optimization.values.toList)
  @Input @Optional def getOptimization: Property[String]
  @Input @Optional def getModuleKind: Property[String]
  @Input @Optional def getModuleSplitStyle: Property[String]
  @Input @Optional def getPrettyPrint: Property[Boolean]

object Link:

  enum Optimization(val description: String) derives CanEqual:
    case Fast extends Optimization(description = " - fast")
    case Full extends Optimization(description = " - full optimization")

  abstract class ModuleInitializerProperties:
    def getName: String // Type must have a read-only 'name' property
    def getClassName: Property[String]
    def getMainMethodName: Property[String]
    def getMainMethodHasArgs: Property[Boolean]

  abstract class Main extends Link:
    final override protected def flavour: String = "Link"
    final override protected def sourceSetName: String = SourceSet.MAIN_SOURCE_SET_NAME
    @Input @Optional def getModuleInitializers: NamedDomainObjectContainer[Link.ModuleInitializerProperties]

  abstract class Test extends Link:
    final override protected def flavour: String = "LinkTest"
    final override protected def sourceSetName: String = SourceSet.TEST_SOURCE_SET_NAME
