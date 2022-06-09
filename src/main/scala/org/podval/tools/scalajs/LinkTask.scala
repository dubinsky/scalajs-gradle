package org.podval.tools.scalajs

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.{OutputDirectory, OutputFile, SourceSet, TaskAction, TaskExecutionException}
import org.opentorah.build.Gradle.*
import org.opentorah.util.Files
import org.scalajs.jsenv.{Input, JSEnv}
import org.scalajs.linker.{PathIRContainer, PathOutputDirectory, StandardImpl}
import org.scalajs.linker.interface.{IRContainer, IRFile, LinkingException, ModuleInitializer, ModuleKind,
  ModuleSplitStyle, Report, Semantics, StandardConfig}
import org.scalajs.testing.adapter.TestAdapterInitializer
import Util.given
import java.io.File
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.*

abstract class LinkTask extends ScalaJSTask:
  protected def sourceSetName: String
  final def getSourceSet(project: Project): SourceSet = project.getSourceSet(sourceSetName)

  protected def sourceSetDescription: String
  setDescription(s"Link ScalaJS${stage.description}$sourceSetDescription")
  setGroup("build")

  private def outputFile(name: String): File = Files.file(
    getProject.getBuildDir,
    "scalajs",
    sourceSetName,
    stage.outputDirectory,
    name
  )

  @OutputDirectory final def getJSDirectory: File = outputFile("js")
  final def reportFile : File = outputFile("report.txt")

  def classesTask: org.gradle.api.Task = getProject.getClassesTask(getSourceSet(getProject))

  // TODO annotate with @Classpath
  private def getRuntimeClassPath: FileCollection = getSourceSet(getProject).getRuntimeClasspath

  getProject.afterEvaluate { (project: Project) =>
    getDependsOn.add(classesTask)
    getInputs.files(getRuntimeClassPath)
    () // return Unit to help the compiler find the correct overload
  }

  // TODO Without the initializers, no JavaScript is emitted - unless entry points are marked in what special way?
  protected def moduleInitializers: Seq[ModuleInitializer]

  @TaskAction def execute(): Unit =
    val outputDirectory: File = getJSDirectory
    val fullOptimization: Boolean = extension.stage == Stage.FullOpt
    val linkerConfig: StandardConfig = StandardConfig()
      .withCheckIR(fullOptimization)
      .withSemantics(if fullOptimization then Semantics.Defaults.optimized else Semantics.Defaults)
      .withModuleKind(extension.moduleKind)
      .withClosureCompiler(fullOptimization && (extension.moduleKind != ModuleKind.ESModule))
      .withModuleSplitStyle(extension.getModuleSplitStyle.byName(ModuleSplitStyle.FewestModules, Util.moduleSplitStyles))
      .withPrettyPrint(extension.getPrettyPrint.getOrElse(false))

    info(
      s"""ScalaJSPlugin:
         |JSDirectory = $outputDirectory
         |ReportFile = $reportFile
         |moduleInitializers = ${moduleInitializers.map(ModuleInitializer.fingerprint).mkString(", ")}
         |linkerConfig = $linkerConfig
         |""".stripMargin,
    )

    outputDirectory.mkdirs()

    try
      val report: Report = Await.result(atMost = Duration.Inf, awaitable = PathIRContainer
        .fromClasspath(getRuntimeClassPath.getFiles.asScala.toSeq.map(_.toPath))
        .map(_._1)
        .flatMap((irContainers: Seq[IRContainer]) => StandardImpl.irFileCache.newCache.cached(irContainers))
        .flatMap((irFiles     : Seq[IRFile]     ) => StandardImpl.linker(linkerConfig).link(
          irFiles = irFiles,
          moduleInitializers = moduleInitializers,
          output = PathOutputDirectory(outputDirectory.toPath),
          logger = jsLogger
        ))
      )

      info(report.toString)

      Files.write(file = reportFile, content = report.toString())
    catch
      case e: LinkingException => throw TaskExecutionException(this, e)


object LinkTask:
  abstract class Main extends LinkTask:
    final override protected def sourceSetName: String = SourceSet.MAIN_SOURCE_SET_NAME
    final override protected def sourceSetDescription: String = ""
    final override protected def moduleInitializers: Seq[ModuleInitializer] = extension.moduleInitializers

  object Main:
    class FastOpt   extends Main with ScalaJSTask.FastOpt
    class FullOpt   extends Main with ScalaJSTask.FullOpt
    class Extension extends Main with ScalaJSTask.Extension

  abstract class Test extends LinkTask:
    final override protected def sourceSetName: String = SourceSet.TEST_SOURCE_SET_NAME
    final override protected def sourceSetDescription: String = " - test"
    // Note: configured moduleInitializers are ignored for tests
    final override protected def moduleInitializers: Seq[ModuleInitializer] = Seq(testModuleInitializer)

    final def testModuleInitializer: ModuleInitializer = ModuleInitializer.mainMethod(
      TestAdapterInitializer.ModuleClassName,
      TestAdapterInitializer.MainMethodName
    )

  object Test:
    class FastOpt   extends Test with ScalaJSTask.FastOpt
    class FullOpt   extends Test with ScalaJSTask.FullOpt
    class Extension extends Test with ScalaJSTask.Extension
