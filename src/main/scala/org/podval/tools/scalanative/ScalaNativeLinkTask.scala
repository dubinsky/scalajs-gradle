package org.podval.tools.scalanative

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{CacheableTask, Input, Optional, OutputDirectory, OutputFile}
import org.podval.tools.nonjvm.LinkTask
import org.podval.tools.util.Tasks
import java.io.File

trait ScalaNativeLinkTask extends LinkTask[ScalaNativeBackend.type]:
  protected def mainClass: Option[String]

  @Input def getMode: Property[String]
  Mode.convention(getMode)
  
  @Input def getLto: Property[String]
  LTO.convention(getLto)

  @Input def getGc: Property[String]
  GC.convention(getGc)

  @Input def getOptimize: Property[Boolean]
  Optimize.convention(getOptimize)

  @Input def getProjectName: Property[String]

  @OutputDirectory final def getNativeDirectory: File = outputDirectory
  
  @OutputFile final def getOutputFile: File = link.artifactPath.toFile

  override lazy val link: ScalaNativeLink = ScalaNativeLink(
    lto = LTO(getLto),
    gc = GC(getGc),
    optimize = Optimize(getOptimize),
    mode = Mode(getMode),
    baseDir = getNativeDirectory.toPath,
    projectName = getProjectName.get,
    mainClass = mainClass,
    isTest = isTest,
    classpath = runtimeClasspath.map(_.toPath),
    // Source-level debugging stays at NativeConfig.empty's disabled default.
    // The sources classpath is consulted only when that is enabled.
    sourcesClasspath = Seq.empty,
    output = output
  )

object ScalaNativeLinkTask:
  def configureTasks(project: Project): Unit =
    val projectName: String = project.getName
    Tasks.configureEach(project, classOf[Main], (task: Main) => task.getProjectName.set(projectName))
    Tasks.configureEach(project, classOf[Test], (task: Test) => task.getProjectName.set(projectName))

  @CacheableTask
  abstract class Main extends LinkTask.Main[ScalaNativeBackend.type] with ScalaNativeLinkTask:
    @Input @Optional def getMainClass: Property[String]
    override protected def mainClass: Option[String] = Option(getMainClass.getOrNull)

  @CacheableTask
  abstract class Test extends LinkTask.Test[ScalaNativeBackend.type] with ScalaNativeLinkTask:
    override protected def mainClass: Option[String] = None
