package org.podval.tools.scalajs

import org.podval.tools.util.Files
import org.scalajs.jsenv.{Input, JSEnv}
import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import org.scalajs.linker.interface.{Report, ModuleKind as ModuleKindSJS}
import org.scalajs.testing.adapter.TestAdapter
import java.nio.file.Path

final class ScalaJSRunCommon(
  val common: ScalaJSCommon,
  nodePath: String,
  nodeEnvironment: Map[String, String]
):
  lazy val mainModule: Report.Module =
    val result: Report.Module = Report
      .deserialize(Files.readBytes(common.reportBinFile))
      .get
      .publicModules
      .find(_.moduleID == "main")
      .getOrElse(throw common.abort(s"Linking report does not have a module named 'main'. See ${common.reportBinFile}"))

    given CanEqual[ModuleKindSJS, ModuleKindSJS] = CanEqual.derived
    require(common.moduleKindSJS == result.moduleKind, s"moduleKind discrepancy: ${common.moduleKind} != ${result.moduleKind}")
    result

  def mainModulePath: Path = Files.file(
    directory = common.jsDirectory,
    segments = mainModule.jsFileName
  ).toPath

  def input: Input = common.moduleKind match
    case ModuleKind.NoModule       => Input.Script        (mainModulePath)
    case ModuleKind.ESModule       => Input.ESModule      (mainModulePath)
    case ModuleKind.CommonJSModule => Input.CommonJSModule(mainModulePath)

  def mkJsEnv: JSEnv = JSDOMNodeJSEnv(JSDOMNodeJSEnv.Config()
    .withExecutable(nodePath)
    .withEnv(nodeEnvironment)
  )

  def createTestAdapter: TestAdapter = TestAdapter(
    jsEnv = mkJsEnv,
    input = Seq(input),
    config = TestAdapter.Config().withLogger(common.loggerJS)
  )
