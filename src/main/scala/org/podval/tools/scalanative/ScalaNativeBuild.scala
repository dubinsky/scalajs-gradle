package org.podval.tools.scalanative

import org.scalajs.linker.interface.LinkingException
import org.slf4j.{Logger, LoggerFactory}
import scala.concurrent.ExecutionContext
import scala.scalanative.build.{Build, BuildException, Config, Discover, GC as GCN, NativeConfig, Logger as LoggerN,
  LTO as LTON, Mode as ModeN}
import scala.scalanative.testinterface.adapter.TestAdapter
import scala.scalanative.util.Scope
import java.io.File
import java.nio.file.Path

// see scala.scalanative.sbtplugin.ScalaNativePluginInternal
object ScalaNativeBuild:
  private val logger: Logger = LoggerFactory.getLogger(ScalaNativeBuild.getClass)
  
  def link(
    lto: LTO,
    gc: GC,
    optimize: Boolean,
    baseDir: Path,
    moduleName: String,
    mode: Mode,
    mainClass: Option[String],
    testConfig: Boolean,
    classpath: Seq[Path],
    sourcesClassPath: Seq[Path]
  ): File =
    val nativeConfig: NativeConfig = NativeConfig.empty
      .withClang(interceptBuildException(Discover.clang()))
      .withClangPP(interceptBuildException(Discover.clangpp()))
      .withCompileOptions(Discover.compileOptions())
      .withLinkingOptions(Discover.linkingOptions())
      .withLTO(toNative(lto))
      .withGC(toNative(gc))
      .withOptimize(optimize)
      .withMode(toNative(mode))
      // TODO .withTargetTriple()

    val config: Config = Config.empty
      .withLogger(nativeLogger)
      .withClassPath(classpath)
      .withSourcesClassPath(sourcesClassPath)
      .withBaseDir(baseDir)
      .withModuleName(moduleName)
      .withMainClass(mainClass)
      .withTestConfig(testConfig)
      .withCompilerConfig(nativeConfig)

    logger.info(s"ScalaNativeBuild.nativeLinkImpl($config)")

    interceptBuildException(
      buildCachedSync(
        config,
        (throwable: Throwable) => logger.warn(s"Trace: $throwable")
      ).toFile
    )
      
  def buildCachedSync(
    config: Config,
    trace: Throwable => Unit
  ): Path =
    implicit val scope: Scope = Scope.forever
    ScalaNativeAwait.await(trace) { implicit ec: ExecutionContext =>
      Build.buildCached(config)
    }

  def createTestAdapter(
    binaryTestFile: File
  ): TestAdapter = TestAdapter(TestAdapter
    .Config()
    .withLogger(nativeLogger)
    .withBinaryFile(binaryTestFile)
  )

  private def nativeLogger: LoggerN = new LoggerN:
    override def trace(msg: Throwable): Unit = logger.trace("", msg)
    override def debug(msg: String): Unit = logger.debug(msg)
    override def info (msg: String): Unit = logger.info (msg)
    override def warn (msg: String): Unit = logger.warn (msg)
    override def error(msg: String): Unit = logger.error(msg)

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

  // TODO is there a Gradle MessageOnlyException analogue?
  /** Run `op`, rethrows `BuildException`s as `MessageOnlyException`s. */
  private def interceptBuildException[T](op: => T): T =
    op
//    try op
//    catch
//      case ex: BuildException => throw new MessageOnlyException(ex.getMessage)
//      case ex: LinkingException => throw new MessageOnlyException(ex.getMessage)
