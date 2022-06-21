package org.podval.tools.scalajs

import org.gradle.api.{Project, Task}
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.{Classpath, OutputDirectory, OutputFile, SourceSet, TaskAction, TaskExecutionException}
import org.opentorah.build.Gradle.*
import org.opentorah.util.Files
import org.scalajs.linker.{PathIRContainer, PathOutputDirectory, StandardImpl}
import org.scalajs.linker.interface.{IRContainer, IRFile, LinkingException, ModuleInitializer, ModuleKind,
  ModuleSplitStyle, Report, Semantics, StandardConfig}
import org.scalajs.testing.adapter.TestAdapterInitializer
import Util.given
import org.gradle.api.tasks.scala.ScalaCompile
import org.podval.tools.scalajs.dependencies.GradleUtil
import sbt.io.IO
import sbt.internal.inc.Analysis
import xsbti.compile.FileAnalysisStore
import java.io.File
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.*

abstract class LinkTask extends ScalaJSTask:
  protected def sourceSetName: String

  private def sourceSet: SourceSet = getProject.getSourceSet(sourceSetName)
  private def classesTask: Task = getProject.getClassesTask(sourceSet)
  private def scalaCompile: ScalaCompile = GradleUtil.getScalaCompile(classesTask)

  private def outputFile(name: String): File = Files.file(
    getProject.getBuildDir,
    "scalajs",
    sourceSetName,
    stage.outputDirectory,
    name
  )

  @Classpath final def getRuntimeClassPath: FileCollection = sourceSet.getRuntimeClasspath
  @OutputDirectory final def getJSDirectory: File = outputFile("js")
  @OutputFile final def getReportTextFile: File = outputFile("linking-report.txt")
  @OutputFile final def getReportBinFile : File = outputFile("linking-report.bin")

  getProject.afterEvaluate { (_: Project) =>
    getDependsOn.add(classesTask)
    // TODO if @Classpath def getRuntimeClassPath works, this is not needed;
    // otherwise - remove @Classpath from it and make it private.
    //getInputs.files(getRuntimeClassPath)
    ()
  }

  final def linkingReport: Option[Report] = Report.deserialize(IO.readBytes(getReportBinFile))

  // Note: scalaCompile.getAnalysisFiles is empty, so I had to hard-code the path:
  final def scalaCompileAnalysisFile: File = Files.file(
    directory = getProject.getBuildDir,
    segments  = s"tmp/scala/compilerAnalysis/${scalaCompile.getName}.analysis"
  )

  final def scalaCompileAnalysis: Analysis = FileAnalysisStore
    .getDefault(scalaCompileAnalysisFile)
    .get
    .get
    .getAnalysis
    .asInstanceOf[Analysis]

  // TODO Without the initializers, no JavaScript is emitted - unless entry points are marked in what special way?
  protected def moduleInitializers: Seq[ModuleInitializer]

  @TaskAction final def execute(): Unit =
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
         |JSDirectory = $getJSDirectory
         |reportFile = $getReportTextFile
         |moduleInitializers = ${moduleInitializers.map(ModuleInitializer.fingerprint).mkString(", ")}
         |linkerConfig = $linkerConfig
         |""".stripMargin,
    )

    getJSDirectory.mkdirs()

    try
      val report: Report = Await.result(atMost = Duration.Inf, awaitable = PathIRContainer
        .fromClasspath(getRuntimeClassPath.getFiles.asScala.toSeq.map(_.toPath))
        .map(_._1)
        .flatMap((irContainers: Seq[IRContainer]) => StandardImpl.irFileCache.newCache.cached(irContainers))
        .flatMap((irFiles     : Seq[IRFile]     ) => StandardImpl.linker(linkerConfig).link(
          irFiles = irFiles,
          moduleInitializers = moduleInitializers,
          output = PathOutputDirectory(getJSDirectory.toPath),
          logger = jsLogger
        ))
      )

      Files.write(file = getReportTextFile, content = report.toString())
      IO.write(getReportBinFile, Report.serialize(report))
    catch
      case e: LinkingException => throw TaskExecutionException(this, e)


object LinkTask:
  abstract class Main extends LinkTask:
    final override protected def flavour: String = "Link"
    final override protected def sourceSetName: String = SourceSet.MAIN_SOURCE_SET_NAME
    final override protected def moduleInitializers: Seq[ModuleInitializer] = extension.moduleInitializers

  object Main:
    class FastOpt   extends Main with ScalaJSTask.FastOpt
    class FullOpt   extends Main with ScalaJSTask.FullOpt
    class Extension extends Main with ScalaJSTask.Extension

  abstract class Test extends LinkTask:
    final override protected def flavour: String = "LinkTest"
    final override protected def sourceSetName: String = SourceSet.TEST_SOURCE_SET_NAME
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
