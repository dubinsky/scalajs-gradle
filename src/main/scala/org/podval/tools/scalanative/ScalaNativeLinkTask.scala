package org.podval.tools.scalanative

import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Optional, OutputDirectory, OutputFile, TaskAction}
import org.podval.tools.build.LinkTask
import org.podval.tools.util.Named
import java.io.File
import java.nio.file.Path

trait ScalaNativeLinkTask extends LinkTask:
  protected def mainClass: Option[String]

  @Input def getMode: Property[String]
  Mode.convention(getMode)
  
  @Input def getLto: Property[String]
  LTO.convention(getLto)

  @Input def getGc: Property[String]
  GC.convention(getGc)

  @Input def getOptimize: Property[Boolean]
  Optimize.convention(getOptimize)

  @OutputDirectory final def getNativeDirectory: File = outputDirectory
  
  @TaskAction final def execute(): Unit = link.link

  @OutputFile final def getOutputFile: File = link.artifactPath.toFile

  private lazy val link: ScalaNativeLink =
    val sourcesClassPath: Seq[Path] = Seq.empty // TODO
    ScalaNativeLink(
      lto = LTO(getLto),
      gc = GC(getGc),
      optimize = Optimize(getOptimize),
      mode = Mode(getMode),
      baseDir = getNativeDirectory.toPath,
      projectName = getProject.getName,
      mainClass = mainClass,
      testConfig = isTest,
      classpath = runtimeClassPath.map(_.toPath),
      sourcesClassPath = sourcesClassPath,
      logSource = getName
    )

object ScalaNativeLinkTask:
  abstract class Main extends LinkTask.Main with ScalaNativeLinkTask:
    @Input @Optional def getMainClass: Property[String]
    override protected def mainClass: Option[String] = Option(getMainClass.getOrNull)

  abstract class Test extends LinkTask.Test with ScalaNativeLinkTask:
    override protected def mainClass: Option[String] = Some("scala.scalanative.testinterface.TestMain")
