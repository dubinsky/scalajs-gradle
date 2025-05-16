package org.podval.tools.scalajsplugin.scalanative

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Internal, Optional, OutputDirectory, OutputFile, TaskAction}
import org.podval.tools.build.Gradle
import org.podval.tools.scalajsplugin.nonjvm.NonJvmLinkTask
import org.podval.tools.scalanative.{GC, LTO, Mode, ScalaNativeBuild, ScalaNativeLink}
import org.podval.tools.util.Named
import java.io.File
import java.nio.file.Path

trait ScalaNativeLinkTask extends NonJvmLinkTask[ScalaNativeLinkTask] with ScalaNativeTask:
  final override protected def buildSubDirectory: String = "scalanative"

  private val projectName: String = getProject.getName

  protected def mainClass: Option[String]

  @Input def getMode: Property[String]
  Mode.convention(getMode)
  def mode: Mode = Mode(getMode)
  
  @Input def getLto: Property[String]
  LTO.convention(getLto)

  @Input def getGc: Property[String]
  GC.convention(getGc)

  @Input def getOptimize: Property[Boolean]
  Named.conventionBoolean(getOptimize, "SCALANATIVE_OPTIMIZE")

  @OutputDirectory final def getNativeDirectory: File = outputFile("native")

  private def moduleName: String = s"$projectName-${mode.name}"

  @TaskAction final def execute(): Unit = linkData.link

  @OutputFile final def getOutputFile: File = linkData.artifactPath.toFile

  // TODO if the main class is not set, link with a different build type to avoid errors!!!
  private lazy val linkData: ScalaNativeLink =
    val sourcesClassPath: Seq[Path] = Seq.empty // TODO
    ScalaNativeLink(
      lto = LTO(getLto),
      gc = GC(getGc),
      optimize = getOptimize.get,
      baseDir = getNativeDirectory.toPath,
      mode = mode,
      moduleName = moduleName,
      mainClass = mainClass,
      testConfig = isTest,
      classpath = runtimeClassPath.map(_.toPath),
      sourcesClassPath = sourcesClassPath
    )
