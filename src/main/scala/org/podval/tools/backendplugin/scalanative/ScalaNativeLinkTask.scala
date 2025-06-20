package org.podval.tools.backendplugin.scalanative

import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, OutputDirectory, OutputFile, TaskAction}
import org.podval.tools.backend.scalanative.{GC, LTO, Mode, Optimize, ScalaNativeBuild, ScalaNativeLinkConfig}
import org.podval.tools.backendplugin.nonjvm.NonJvmLinkTask
import org.podval.tools.util.Named
import java.io.File
import java.nio.file.Path

trait ScalaNativeLinkTask extends NonJvmLinkTask[ScalaNativeLinkTask] with ScalaNativeTask:
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
  
  @TaskAction final def execute(): Unit = linkConfig.link(getName, abort)

  @OutputFile final def getOutputFile: File = linkConfig.artifactPath.toFile

  private lazy val linkConfig: ScalaNativeLinkConfig =
    val sourcesClassPath: Seq[Path] = Seq.empty // TODO
    ScalaNativeBuild.linkConfig(
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
      abort = abort
    )
