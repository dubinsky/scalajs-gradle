package org.podval.tools.scalajs

import org.gradle.api.{Project, Task as GTask}
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.{Classpath, OutputDirectory, OutputFile, SourceSet}
import org.opentorah.build.Gradle.*
import org.opentorah.util.Files
import java.io.File

abstract class LinkTask extends ScalaJSTask:
  protected def sourceSetName: String
  private def sourceSet: SourceSet = getProject.getSourceSet(sourceSetName)
  final def classesTask: GTask = getProject.getClassesTask(sourceSet)

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

  final override protected def doExecute(actions: Actions): Unit = actions.link()

object LinkTask:
  abstract class Main extends LinkTask:
    final override protected def flavour: String = "Link"
    final override protected def sourceSetName: String = SourceSet.MAIN_SOURCE_SET_NAME

  object Main:
    class FastOpt       extends Main with ScalaJSTask.FastOpt
    class FullOpt       extends Main with ScalaJSTask.FullOpt
    class FromExtension extends Main with ScalaJSTask.FromExtension

  abstract class Test extends LinkTask:
    final override protected def flavour: String = "LinkTest"
    final override protected def sourceSetName: String = SourceSet.TEST_SOURCE_SET_NAME

  object Test:
    class FastOpt       extends Test with ScalaJSTask.FastOpt
    class FullOpt       extends Test with ScalaJSTask.FullOpt
    class FromExtension extends Test with ScalaJSTask.FromExtension
