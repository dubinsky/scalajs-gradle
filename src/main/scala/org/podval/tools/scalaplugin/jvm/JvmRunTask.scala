package org.podval.tools.scalaplugin.jvm

import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Optional, TaskAction}
import org.gradle.process.{ExecOperations, JavaExecSpec}
import org.podval.tools.platform.OutputPiper
import org.podval.tools.scalaplugin.BackendTask
import javax.inject.Inject

abstract class JvmRunTask @Inject(execOperations: ExecOperations)
  extends BackendTask.Run.Main
  with JvmTask
  with BackendTask.HasRuntimeClassPath
  with BackendTask.DependsOnClasses:
  
  @Input @Optional def getMainClass: Property[String]

  @TaskAction final def execute(): Unit = Option(getMainClass.getOrNull) match
    case None => 
      getLogger.warn(s"JvmRunTask: not running: mainClass is not set.")
      
    case Some(mainClass) =>
      OutputPiper.run(outputHandler, mainClass): (outputPiper: OutputPiper) =>
        execOperations.javaexec: (exec: JavaExecSpec) =>
          exec.setClasspath(getRuntimeClassPath)
          exec.getMainClass.set(mainClass)
          outputPiper.start(exec)
