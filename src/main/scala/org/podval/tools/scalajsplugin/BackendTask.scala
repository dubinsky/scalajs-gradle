package org.podval.tools.scalajsplugin

import org.gradle.api.{DefaultTask, Task}
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.{InputFiles, Internal}
import org.podval.tools.files.OutputHandler
import org.podval.tools.test.task.TestTask
import scala.jdk.CollectionConverters.SetHasAsScala
import java.io.File

trait BackendTask extends Task:
  @Internal def isTest: Boolean

object BackendTask:
  sealed trait Main extends BackendTask:
    override def isTest: Boolean = false

  sealed trait Test extends BackendTask:
    override def isTest: Boolean = true

  trait DependsOnClasses extends BackendTask

  trait HasRuntimeClassPath extends BackendTask:
    @InputFiles def getRuntimeClassPath: ConfigurableFileCollection
    final def runtimeClassPath: Seq[File] = getRuntimeClassPath.getFiles.asScala.toSeq

  sealed trait Link extends BackendTask

  object Link:
    abstract class Main extends DefaultTask with Link with BackendTask.Main
    abstract class Test extends DefaultTask with Link with BackendTask.Test

  sealed trait Run extends BackendTask

  object Run:
    abstract class Main extends DefaultTask with Run with BackendTask.Main:
      final def outputHandler: OutputHandler = OutputHandler(getLogger)

    abstract class Test extends TestTask    with Run with BackendTask.Test
