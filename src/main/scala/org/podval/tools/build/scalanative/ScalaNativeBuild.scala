package org.podval.tools.build.scalanative

import org.podval.tools.build.nonjvm.NonJvmTestAdapter
import org.slf4j.{Logger, LoggerFactory}
import sbt.testing.Framework
import scala.concurrent.ExecutionContext
import scala.scalanative.build.{Build, Config, Discover, GC as GCN, LTO as LTON, Logger as LoggerN, Mode as ModeN,
  NativeConfig}
import scala.scalanative.testinterface.adapter.TestAdapter
import scala.scalanative.util.Scope
import java.io.File
import java.nio.file.Path

// TODO add abort
// TODO merge into backend

// see scala.scalanative.sbtplugin.ScalaNativePluginInternal
object ScalaNativeBuild:
  val logger: Logger = LoggerFactory.getLogger(ScalaNativeBuild.getClass)

  def linkConfig(
    lto: LTO,
    gc: GC,
    optimize: Boolean,
    baseDir: Path,
    projectName: String,
    mode: Mode,
    mainClass: Option[String],
    testConfig: Boolean,
    classpath: Seq[Path],
    sourcesClassPath: Seq[Path]
  ): ScalaNativeLinkConfig =
    val moduleName: String = s"$projectName-${mode.name}"

  // TODO if the main class is not set, link with a different build type to avoid errors!!!
    val nativeConfig: NativeConfig = NativeConfig.empty
      .withClang(ScalaNativeBuild.interceptBuildException(Discover.clang()))
      .withClangPP(ScalaNativeBuild.interceptBuildException(Discover.clangpp()))
      .withCompileOptions(Discover.compileOptions())
      .withLinkingOptions(Discover.linkingOptions())
      .withLTO(toNative(lto))
      .withGC(toNative(gc))
      .withOptimize(optimize)
      .withMode(toNative(mode))
    // TODO .withTargetTriple()

    val config: Config = Config.empty
      .withClassPath(classpath)
      .withSourcesClassPath(sourcesClassPath)
      .withBaseDir(baseDir)
      .withModuleName(moduleName)
      .withMainClass(mainClass)
      .withTestConfig(testConfig)
      .withCompilerConfig(nativeConfig)

    ScalaNativeLinkConfig(config)

  def link(
    config: Config,
    logSource: String
  ): Path =
    logger.info(s"ScalaNativeBuild.nativeLinkImpl($config)")

    interceptBuildException(
      buildCachedSync(
        config.withLogger(loggerN(logSource)),
        (throwable: Throwable) => logger.warn(s"Trace: $throwable")
      )
    )
    
  private def buildCachedSync(
    config: Config,
    trace: Throwable => Unit
  ): Path =
    implicit val scope: Scope = Scope.forever
    ScalaNativeAwait.await(trace) { implicit ec: ExecutionContext =>
      Build.buildCached(config)
    }

  def createTestEnvironment(
    binaryTestFile: File,
    logSource: String
  ): ScalaNativeTestEnvironment = ScalaNativeTestEnvironment(
    sourceMapper = None, // TODO
    testAdapter = new NonJvmTestAdapter:
      override def loadFrameworks(frameworkNames: List[List[String]]): List[Option[Framework]] =
        testAdapter.loadFrameworks(frameworkNames)
      override def close(): Unit = testAdapter.close()
      private lazy val testAdapter: TestAdapter = TestAdapter(TestAdapter
        .Config()
        .withLogger(loggerN(logSource))
        .withBinaryFile(binaryTestFile)
      )
  )

  // TODO is there a Gradle MessageOnlyException analogue?

  /** Run `op`, rethrows `BuildException`s as `MessageOnlyException`s. */
  private def interceptBuildException[T](op: => T): T =
    op
  //    try op
  //    catch
  //      case ex: BuildException => throw new MessageOnlyException(ex.getMessage)
  //      case ex: LinkingException => throw new MessageOnlyException(ex.getMessage)

  private def loggerN(logSource: String): LoggerN = new LoggerN:
    def toLog(message: String): String = s"$logSource: $message"

    override def trace(throwable: Throwable): Unit = logger.error(s"$logSource Error", throwable)
    override def debug(msg: String): Unit = logger.debug(toLog(msg))
    override def info (msg: String): Unit = logger.info (toLog(msg))
    override def warn (msg: String): Unit = logger.warn (toLog(msg))
    override def error(msg: String): Unit = logger.error(toLog(msg))
  
  private def toNative(lto: LTO): LTON = lto match
    case LTO.None => LTON.none
    case LTO.Thin => LTON.thin
    case LTO.Full => LTON.full

  private def toNative(gc: GC): GCN = gc match
    case GC.None   => GCN.none
    case GC.Boehm  => GCN.boehm
    case GC.Immix  => GCN.immix
    case GC.Commix => GCN.commix

  private def toNative(mode: Mode): ModeN = mode match
    case Mode.Debug       => ModeN.debug
    case Mode.ReleaseFast => ModeN.releaseFast
    case Mode.ReleaseFull => ModeN.releaseFull
    case Mode.ReleaseSize => ModeN.releaseSize
