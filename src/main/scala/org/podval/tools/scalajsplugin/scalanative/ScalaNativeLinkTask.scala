package org.podval.tools.scalajsplugin.scalanative

import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, OutputDirectory, OutputFile, TaskAction}
import org.podval.tools.build.scalanative.{GC, LTO, Mode, ScalaNativeBuild, ScalaNativeLinkConfig}
import org.podval.tools.scalajsplugin.nonjvm.NonJvmLinkTask
import org.podval.tools.util.Named
import java.io.File
import java.nio.file.Path

trait ScalaNativeLinkTask extends NonJvmLinkTask[ScalaNativeLinkTask] with ScalaNativeTask:
  final override protected def buildSubDirectory: String = "scalanative"
  
  protected def mainClass: Option[String]

  @Input def getMode: Property[String]
  Mode.convention(getMode)
  
  @Input def getLto: Property[String]
  LTO.convention(getLto)

  @Input def getGc: Property[String]
  GC.convention(getGc)

  @Input def getOptimize: Property[Boolean]
  Named.conventionBoolean(getOptimize, "SCALANATIVE_OPTIMIZE")

  @OutputDirectory final def getNativeDirectory: File = outputFile("native")
  
  @TaskAction final def execute(): Unit = linkConfig.link(getName)

  @OutputFile final def getOutputFile: File = linkConfig.artifactPath.toFile

  private lazy val linkConfig: ScalaNativeLinkConfig =
    val sourcesClassPath: Seq[Path] = Seq.empty // TODO
    ScalaNativeBuild.linkConfig(
      lto = LTO(getLto),
      gc = GC(getGc),
      optimize = getOptimize.get,
      baseDir = getNativeDirectory.toPath,
      mode = Mode(getMode),
      projectName = getProject.getName,
      mainClass = mainClass,
      testConfig = isTest,
      classpath = runtimeClassPath.map(_.toPath),
      sourcesClassPath = sourcesClassPath
    )
