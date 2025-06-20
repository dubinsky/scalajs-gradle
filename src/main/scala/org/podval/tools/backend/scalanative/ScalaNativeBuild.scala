package org.podval.tools.backend.scalanative

import org.podval.tools.backend.nonjvm.NonJvmTestAdapter
import org.scalajs.linker.interface.LinkingException
import org.slf4j.{Logger, LoggerFactory}
import sbt.testing.Framework
import scala.scalanative.build.{Build, BuildException, BuildTarget, Config, Discover, NativeConfig, GC as GCN,
  LTO as LTON, Logger as LoggerN, Mode as ModeN}
import scala.scalanative.testinterface.adapter.TestAdapter
import scala.scalanative.util.Scope
import java.io.File
import java.nio.file.Path

// see scala.scalanative.sbtplugin.ScalaNativePluginInternal
// https://github.com/scala-native/scala-native/blob/main/sbt-scala-native/src/main/scala/scala/scalanative/sbtplugin/ScalaNativePluginInternal.scala
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
    sourcesClassPath: Seq[Path],
    abort: String => Nothing
  ): ScalaNativeLinkConfig =
    val moduleName: String = s"$projectName-${mode.name}"
    val buildTarget: BuildTarget = mainClass match
      case Some(_) => BuildTarget.application
      case None => BuildTarget.libraryDynamic

    val nativeConfig: NativeConfig = NativeConfig.empty
      .withBuildTarget(buildTarget)
      .withClang(ScalaNativeBuild.interceptBuildException(Discover.clang(), abort))
      .withClangPP(ScalaNativeBuild.interceptBuildException(Discover.clangpp(), abort))
      .withCompileOptions(Discover.compileOptions())
      .withLinkingOptions(Discover.linkingOptions())
// TODO     .withTargetTriple()
//      .withSourceLevelDebuggingConfig()
      .withLTO(toNative(lto))
      .withGC(toNative(gc))
      .withOptimize(optimize)
      .withMode(toNative(mode))

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
    logSource: String,
    abort: String => Nothing
  ): Path =
    logger.info(s"ScalaNativeBuild.link($config)")

    implicit val scope: Scope = Scope.forever
    interceptBuildException(
      Build.buildCachedAwait(config
        .withLogger(loggerN(logSource))
      ),
      abort
    )

  def createTestEnvironment(
    binaryTestFile: File,
    logSource: String
  ): ScalaNativeTestEnvironment = ScalaNativeTestEnvironment(
    sourceMapper = None,
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

  /** Run `op`, rethrows `BuildException`s as `MessageOnlyException`s. */
  private def interceptBuildException[T](op: => T, abort: String => Nothing): T =
    try op catch
      case ex: BuildException   => abort(ex.getMessage)
      case ex: LinkingException => abort(ex.getMessage)

  private def loggerN(logSource: String): LoggerN = new LoggerN:
    def toLog(level: Char, message: String): String = s"$logSource[$level]: $message"

    override def trace(throwable: Throwable): Unit = logger.error(s"$logSource Error", throwable)
    override def debug(msg: String): Unit = logger.debug(toLog('d', msg))
    override def info (msg: String): Unit = logger.info (toLog('i', msg))
    override def warn (msg: String): Unit = logger.warn (toLog('w', msg))
    override def error(msg: String): Unit = logger.error(toLog('e', msg))
  
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
