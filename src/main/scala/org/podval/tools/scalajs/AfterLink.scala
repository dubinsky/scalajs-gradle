package org.podval.tools.scalajs

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.opentorah.util.Files
import org.scalajs.jsenv.{Input, JSEnv}
import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import org.scalajs.linker.interface.{ModuleKind, Report}
import sbt.io.IO
import java.io.File
import java.nio.file.Path

final class AfterLink(
  moduleKindProperty: Property[String],
  reportBinFile: File,
  jsDirectory: File,
  taskName: String,
  logger: Logger,
  nodePath: String
):
  private given CanEqual[ModuleKind, ModuleKind] = CanEqual.derived

  val moduleKind: ModuleKind = Link.moduleKind(moduleKindProperty)

  val mainModule: Report.Module =
    val result: Report.Module = Report
      .deserialize(IO.readBytes(reportBinFile))
      .get
      .publicModules
      .find(_.moduleID == "main")
      .getOrElse(throw GradleException(s"Linking result does not have a module named 'main'. See $reportBinFile"))

    require(moduleKind == result.moduleKind, s"moduleKind discrepancy: $moduleKind != ${result.moduleKind}")
    result

  private lazy val mainModulePath: Path = Files.file(
    directory = jsDirectory,
    segments = mainModule.jsFileName
  ).toPath

  lazy val input: Input = moduleKind match
    case ModuleKind.NoModule       => Input.Script        (mainModulePath)
    case ModuleKind.ESModule       => Input.ESModule      (mainModulePath)
    case ModuleKind.CommonJSModule => Input.CommonJSModule(mainModulePath)

  // Note: if moved into the caller breaks class loading
  lazy val jsLogger: org.scalajs.logging.Logger = ScalaJSLogger(taskName, logger)

  // Note: if moved into the caller breaks class loading
  lazy val jsEnv: JSEnv = JSDOMNodeJSEnv(JSDOMNodeJSEnv.Config().withEnv(Map(
    "NODE_PATH" -> nodePath
  )))
