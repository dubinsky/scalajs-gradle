package org.podval.tools.scalajs

//import jsenv.playwright.PWEnv
import org.gradle.process.ExecOperations
import org.podval.tools.node.NodeProject
import org.podval.tools.nonjvm.{NonJvmTestEnvironment, Run}
import org.podval.tools.platform.{Files, Output, Runner}
import org.podval.tools.test.task.TestEnvironment
import org.scalajs.jsenv.{Input, JSEnv, JSRun, RunConfig}
import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import org.scalajs.jsenv.nodejs.NodeJSEnv
import org.scalajs.linker.interface.Report
import org.scalajs.testing.adapter.TestAdapter
import java.io.InputStream
import java.nio.file.Path
import scala.concurrent.Await
import scala.concurrent.duration.Duration

final class ScalaJSRun(
  val jsEnvKind: JSEnvKind,
  nodeProject: NodeProject,
  browserName: BrowserName,
  link: ScalaJSLink,
  output: Output
) extends ScalaJSBuild(output) with Run[ScalaJSBackend.type]:
  override def run(runner: Runner): Unit =
    val (_: Report.Module, modulePath: Path, input: Input) = link.module(jsEnvKind)

    runner.run(modulePath.toString, log = true):
      /* #4560 Explicitly redirect out/err to System.out/System.err, instead
       * of relying on `inheritOut` and `inheritErr`, so that streams
       * installed with `System.setOut` and `System.setErr` are always taken
       * into account. sbt installs such alternative outputs when it runs in
       * server mode.
       */
      val jsRun: JSRun = jsEnv.start(
        input = Seq(input),
        config = RunConfig()
          .withInheritOut(false)
          .withInheritErr(false)
          .withOnOutputStream((out: Option[InputStream], err: Option[InputStream]) => runner.start(out, err))
          .withLogger(backendLogger)
      )
      Await.result(awaitable = jsRun.future, atMost = Duration.Inf)
      jsRun.close()

  override def testEnvironment: TestEnvironment[ScalaJSBackend.type] =
    val (module: Report.Module, _: Path, input: Input) = link.module(jsEnvKind)

    NonJvmTestEnvironment[ScalaJSBackend.type, TestAdapter](
      backend = ScalaJSBackend,
      testAdapter = TestAdapter(
        jsEnv = jsEnv,
        input = Seq(input),
        config = TestAdapter.Config().withLogger(backendLogger)
      ),
      loadFrameworksFromTestAdapter = _.loadFrameworks(_),
      closeTestAdapter = _.close(),
      sourceMapper = module
        .sourceMapName
        .map((name: String) => Files.file(link.jsDirectory, name))
        .map(ClosureCompilerSourceMapper(_))
    )

  private def jsEnv: JSEnv =
    val executable: String = nodeProject.node.node.getAbsolutePath
    val env: Map[String, String] = nodeProject.nodeEnv.toMap

    // see https://www.scala-js.org/doc/project/webassembly.html
    def args: List[String] = if !link.useWebAssembly then List.empty else List(
      "--experimental-wasm-exnref", // always required
      "--experimental-wasm-jspi", // required for js.async/js.await
      "--experimental-wasm-imported-strings", // optional (good for performance)
    )

    jsEnvKind match
      case JSEnvKind.NodeJS =>
        val config: NodeJSEnv.Config = NodeJSEnv.Config()
          .withExecutable(executable)
          .withEnv(env)
          .withArgs(args)
        debug(
          s"""jsEnv=NodeJSEnv(
             |  executable=${config.executable},
             |  env=${config.env},
             |  args=${config.args},
             |  sourceMap=${config.sourceMap}
             |)
             |""".stripMargin
        )
        NodeJSEnv(config)

      case JSEnvKind.JSDOMNodeJS =>
        val config: JSDOMNodeJSEnv.Config = JSDOMNodeJSEnv.Config()
          .withExecutable(executable)
          .withEnv(env)
          .withArgs(args)
        debug(
          s"""jsEnv=JSDOMNodeJSEnv(
             |  executable=${config.executable},
             |  env=${config.env},
             |  args=${config.args}
             |)
             |""".stripMargin
        )
        JSDOMNodeJSEnv(config)

      case JSEnvKind.Playwright =>
        throw new IllegalArgumentException(s"Playwright JavaScript environment is not supported until io.github.gmkumar2005:scala-js-env-playwrights_2.13 artifacts start being published.")
//        val config: PWEnv.Config = PWEnv.Config()
//        ScalaJSRunConfig.logger.debug(s"$logSource: jsEnv=PWEnv($config), browserName=$browserName")
//        PWEnv(
//          browserName = browserName.name,
////          headless = ???,
////          showLogs = ???,
////          debug = ???,
//          pwConfig = config
//        )
