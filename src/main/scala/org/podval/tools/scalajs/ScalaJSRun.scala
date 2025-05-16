package org.podval.tools.scalajs

import org.podval.tools.node.Node
import org.podval.tools.platform.{OutputHandler, OutputPiper}
import org.podval.tools.util.Files
import org.scalajs.jsenv.{Input, JSEnv, RunConfig}
import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import org.scalajs.linker.interface.ModuleKind as ModuleKindSJS
import org.scalajs.linker.interface.{Report, ModuleKind as ModuleKindSJS}
import java.io.{File, InputStream}
import java.nio.file.Path

final class ScalaJSRun(
  jsDirectory: File,
  reportBinFile: File,
  moduleKind: ModuleKind,
  abort: String => Nothing
):
  private val mainModule: Report.Module =
    val result: Report.Module = Report
      .deserialize(Files.readBytes(reportBinFile))
      .get
      .publicModules
      .find(_.moduleID == "main")
      .getOrElse(abort(s"Linking report does not have a module named 'main'. See $reportBinFile."))

    given CanEqual[ModuleKindSJS, ModuleKindSJS] = CanEqual.derived
    require(
      ScalaJSBuild.toSJS(moduleKind) == result.moduleKind,
      s"moduleKind discrepancy: $moduleKind != ${result.moduleKind}"
    )
    result

  private val mainModulePath: Path = Files.file(
    directory = jsDirectory,
    segments = mainModule.jsFileName
  ).toPath

  private val input: Input = moduleKind match
    case ModuleKind.NoModule       => Input.Script        (mainModulePath)
    case ModuleKind.ESModule       => Input.ESModule      (mainModulePath)
    case ModuleKind.CommonJSModule => Input.CommonJSModule(mainModulePath)

  // TODO move into Build
  private def mkJsEnv(node: Node): JSEnv = JSDOMNodeJSEnv(JSDOMNodeJSEnv.Config()
    .withExecutable(node.installation.node.getAbsolutePath)
    .withEnv(node.nodeEnv.toMap)
  )

  def run(
    outputHandler: OutputHandler,
    node: Node,
    logSource: String
  ): Unit =
    val jsEnv: JSEnv = mkJsEnv(node)

    OutputPiper.run(
      outputHandler,
      running = s"$mainModulePath on ${jsEnv.name}"
    ): (outputPiper: OutputPiper) =>
      /* #4560 Explicitly redirect out/err to System.out/System.err, instead
       * of relying on `inheritOut` and `inheritErr`, so that streams
       * installed with `System.setOut` and `System.setErr` are always taken
       * into account. sbt installs such alternative outputs when it runs in
       * server mode.
       */
      ScalaJSBuild.run(
        jsEnv = jsEnv,
        input = input,
        logSource = logSource,
        config = RunConfig()
          .withInheritOut(false)
          .withInheritErr(false)
          .withOnOutputStream((out: Option[InputStream], err: Option[InputStream]) => outputPiper.start(out, err))
      )

  def createTestEnvironment(
    jsDirectory: File,
    node: Node,
    logSource: String
  ): ScalaJSTestEnvironment = ScalaJSBuild.createTestEnvironment(
    jsDirectory = jsDirectory,
    jsEnv = mkJsEnv(node),
    mainModule = mainModule,
    input = input,
    logSource = logSource
  )
