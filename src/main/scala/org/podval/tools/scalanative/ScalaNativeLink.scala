package org.podval.tools.scalanative

import java.nio.file.Path
import scala.scalanative.build.{Build, BuildTarget, Config, Discover, NativeConfig, GC as GCN, LTO as LTON, Mode as ModeN}
import scala.scalanative.util.Scope

final class ScalaNativeLink(
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
  logSource: String
) extends ScalaNativeBuild(
  logSource
):
  private val config: Config =
    val moduleName: String = s"$projectName-${mode.name}"
    val buildTarget: BuildTarget = mainClass match
      case Some(_) => BuildTarget.application
      case None => BuildTarget.libraryDynamic

    val nativeConfig: NativeConfig = NativeConfig.empty
      .withBuildTarget(buildTarget)
      .withClang(interceptBuildException(Discover.clang()))
      .withClangPP(interceptBuildException(Discover.clangpp()))
      .withCompileOptions(Discover.compileOptions())
      .withLinkingOptions(Discover.linkingOptions())
      .withLTO(ScalaNativeLink.toNative(lto))
      .withGC(ScalaNativeLink.toNative(gc))
      .withOptimize(optimize)
      .withMode(ScalaNativeLink.toNative(mode))

    Config.empty
      .withClassPath(classpath)
      .withSourcesClassPath(sourcesClassPath)
      .withBaseDir(baseDir)
      .withModuleName(moduleName)
      .withMainClass(mainClass)
      .withTestConfig(testConfig)
      .withCompilerConfig(nativeConfig)

  def artifactName: String = config.artifactName
  def artifactPath: Path   = config.artifactPath
  
  def link: Path =
    logger.info(s"ScalaNativeLink.link($config)")

    implicit val scope: Scope = Scope.forever
    interceptBuildException(
      Build.buildCachedAwait(config
        .withLogger(loggerN)
      )
    )

object ScalaNativeLink:
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
