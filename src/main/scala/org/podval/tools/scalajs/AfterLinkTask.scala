package org.podval.tools.scalajs

import org.gradle.api.{GradleException, Project}
import org.opentorah.util.Files
import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import org.scalajs.jsenv.{Input, JSEnv}
import org.scalajs.linker.interface.{ModuleKind, Report}
import Util.given
import java.nio.file.Path

abstract class AfterLinkTask[T <: LinkTask](clazz: Class[T]) extends ScalaJSTask:

  // Find corresponding LinkTask and depend on it:
  final protected def linkTask: LinkTask =
    getProject.getTasks.withType(clazz).iterator.next

  getProject.afterEvaluate { (_: Project) =>
    getDependsOn.add(linkTask)
    () // return Unit to help the compiler find the correct overload
  }

  final protected def jsEnv: JSEnv = new JSDOMNodeJSEnv()

  final protected def mainModulePath: Path =
    val report: Report = linkTask.linkingReport.get
    val mainModule: Report.Module = report.publicModules.find(_.moduleID == "main")
      .getOrElse(throw GradleException(s"Linking result does not have a module named 'main'. Full report:\n$report"))
    Files.file(linkTask.getJSDirectory, mainModule.jsFileName).toPath

  final protected def input: Input = extension.moduleKind match
    case ModuleKind.NoModule       => Input.Script        (mainModulePath)
    case ModuleKind.ESModule       => Input.ESModule      (mainModulePath)
    case ModuleKind.CommonJSModule => Input.CommonJSModule(mainModulePath)
