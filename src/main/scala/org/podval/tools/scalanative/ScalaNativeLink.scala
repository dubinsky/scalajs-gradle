package org.podval.tools.scalanative

import scala.scalanative.build.{Config, Discover, NativeConfig, GC as GCN, LTO as LTON, Mode as ModeN}
import java.io.File
import java.nio.file.Path

// see scala.scalanative.sbtplugin.ScalaNativePluginInternal
final class ScalaNativeLink(
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
):
  // TODO pass individual parameters to ScalaNativeBuild and convert there
  private val nativeConfig: NativeConfig = NativeConfig.empty
    .withClang(ScalaNativeBuild.interceptBuildException(Discover.clang()))
    .withClangPP(ScalaNativeBuild.interceptBuildException(Discover.clangpp()))
    .withCompileOptions(Discover.compileOptions())
    .withLinkingOptions(Discover.linkingOptions())
    .withLTO(ScalaNativeLink.toNative(lto))
    .withGC(ScalaNativeLink.toNative(gc))
    .withOptimize(optimize)
    .withMode(ScalaNativeLink.toNative(mode))
  // TODO .withTargetTriple()

  private val config: Config = Config.empty
    .withClassPath(classpath)
    .withSourcesClassPath(sourcesClassPath)
    .withBaseDir(baseDir)
    .withModuleName(moduleName)
    .withMainClass(mainClass)
    .withTestConfig(testConfig)
    .withCompilerConfig(nativeConfig)

  def artifactName: String = config.artifactName
  def artifactPath: Path = config.artifactPath
  
  def link: Path = ScalaNativeBuild.link(config)

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
    case Mode.Debug => ModeN.debug
    case Mode.ReleaseFast => ModeN.releaseFast
    case Mode.ReleaseFull => ModeN.releaseFull
    case Mode.ReleaseSize => ModeN.releaseSize
