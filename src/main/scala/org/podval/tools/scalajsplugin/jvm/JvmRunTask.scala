package org.podval.tools.scalajsplugin.jvm

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, InputFiles, Optional, TaskAction}
import org.gradle.process.{ExecOperations, JavaExecSpec}
import org.podval.tools.build.Gradle
import org.slf4j.{Logger, LoggerFactory}
import javax.inject.Inject

object JvmRunTask:
  private val logger: Logger = LoggerFactory.getLogger(classOf[JvmRunTask])
  
abstract class JvmRunTask @Inject(execOperations: ExecOperations) extends DefaultTask:
  @InputFiles def getRuntimeClassPath: ConfigurableFileCollection

  @Input @Optional def getMainClass: Property[String]

  @TaskAction final def execute(): Unit = Gradle.toOption(getMainClass) match
    case None =>
      JvmRunTask.logger.warn(s"JvmRunTask: not running: mainClass is not set.")
      
    case Some(mainClass) =>
      JvmRunTask.logger.info(s"Running $mainClass.")
      execOperations.javaexec: (exec: JavaExecSpec) =>
        exec.setClasspath(getRuntimeClassPath)
        exec.getMainClass.set(mainClass)
        ()
      JvmRunTask.logger.info(s"Done running $mainClass.")
