package org.podval.tools.scalanative

import org.podval.tools.nonjvm.Link
import org.podval.tools.platform.Output
import scala.scalanative.build.{Build, BuildTarget, Config, Discover, NativeConfig, GC as GCN, LTO as LTON, Mode as ModeN}
import scala.scalanative.util.Scope
import java.nio.file.Path

final class ScalaNativeLink(
  lto: LTO,
  gc: GC,
  optimize: Boolean,
  baseDir: Path,
  projectName: String,
  mode: Mode,
  mainClass: Option[String],
  isTest: Boolean,
  classpath: Seq[Path],
  sourcesClasspath: Seq[Path],
  output: Output
) extends ScalaNativeBuild(output) with Link[ScalaNativeBackend.type]:
  private val config: Config =
    val moduleName: String = s"$projectName-${mode.name}"

    val mainClassEffective: Option[String] = mainClass.orElse:
      if !isTest then None else Some("scala.scalanative.testinterface.TestMain")

    val buildTarget: BuildTarget = mainClassEffective match
      case Some(_) => BuildTarget.application
      case None => BuildTarget.libraryDynamic

    val nativeConfig: NativeConfig = NativeConfig.empty
      .withBuildTarget(buildTarget)
      .withClang(interceptException(Discover.clang()))
      .withClangPP(interceptException(Discover.clangpp()))
      .withCompileOptions(Discover.compileOptions())
      .withLinkingOptions(Discover.linkingOptions())
      .withLTO(ScalaNativeLink.toNative(lto))
      .withGC(ScalaNativeLink.toNative(gc))
      .withOptimize(optimize)
      .withMode(ScalaNativeLink.toNative(mode))

    Config.empty
      .withClassPath(classpath)
      .withSourcesClassPath(sourcesClasspath)
      .withBaseDir(baseDir)
      .withModuleName(moduleName)
      .withMainClass(mainClassEffective)
      .withTestConfig(isTest)
      .withCompilerConfig(nativeConfig)

  def artifactName: String = config.artifactName
  def artifactPath: Path   = config.artifactPath
  
  override def link(): Unit =
    debug(s"ScalaNativeLink.link($config)")

    implicit val scope: Scope = Scope.forever
    interceptException(
      Build.buildCachedAwait(config
        .withLogger(backendLogger)
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
