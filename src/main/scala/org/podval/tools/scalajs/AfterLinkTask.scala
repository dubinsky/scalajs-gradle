package org.podval.tools.scalajs

import org.gradle.api.Project
import org.opentorah.util.Files
import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import org.scalajs.jsenv.{Input, JSEnv}
import org.scalajs.linker.interface.ModuleKind
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

  // TODO verify that moduleId 'main' is exist in the Linker report (as sbt plugin does);
  // that report needs to be serialized in LinkTask and deserialized here...

  final protected def path: Path = Files.file(linkTask.getJSDirectory, "main.js").toPath

  final protected def input: Input = extension.moduleKind match
    case ModuleKind.NoModule       => Input.Script        (path)
    case ModuleKind.ESModule       => Input.ESModule      (path)
    case ModuleKind.CommonJSModule => Input.CommonJSModule(path)
