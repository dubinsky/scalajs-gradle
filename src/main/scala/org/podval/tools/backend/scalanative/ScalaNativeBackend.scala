package org.podval.tools.backend.scalanative

import org.podval.tools.backend.ScalaBackend
import org.podval.tools.backend.jvm.JvmBackend
import org.podval.tools.backend.nonjvm.NonJvmBackend
import org.podval.tools.build.{Dependency, DependencyRequirement, ScalaBinaryVersion, ScalaDependency, 
  ScalaVersion, Version}
import org.podval.tools.test.framework.JUnit4ScalaNative

case object ScalaNativeBackend extends NonJvmBackend:
  override val name: String = "Scala Native"
  override val sourceRoot: String = "native"
  override val artifactSuffix: String = "native0.5"
  override val versionDefault: Version.Simple = Version.Simple("0.5.8")

  override def scalaCompileParameters(scalaVersion: ScalaVersion): Seq[String] =
    if scalaVersion.binaryVersion == ScalaBinaryVersion.Scala213
    then Seq("-Ytasty-reader")
    else Seq.empty
    
  override def areCompilerPluginsBuiltIntoScala3: Boolean = false
  override def junit4: Dependency.Maker = JUnit4ScalaNative.forNative.get.maker
  override def versionExtractor(version: Version): Version.Simple = version.compound.right
  
  override def versionComposer(
    projectScalaVersion: ScalaVersion,
    backendVersion: Version.Simple
  ): Version = new Version.Compound(
    projectScalaVersion.version,
    backendVersion
  )

  private val group: String = "org.scala-native"

  private sealed class Maker(
    final override val scalaBackend: ScalaBackend,
    final override val artifact: String,
    what: String,
    final override val isScalaVersionFull: Boolean = false
  ) extends ScalaDependency.Maker:
    final override def description: String = describe(what)
    final override def versionDefault: Version.Simple = ScalaNativeBackend.versionDefault
    final override def group: String = ScalaNativeBackend.group

  private sealed class ScalaNativeMaker(
    artifact: String,
    what: String
  ) extends Maker(
    ScalaNativeBackend,
    artifact, 
    what
  )
  
  override def implementation: Array[ScalaDependency.Maker] = Array(
    ScalaNativeMaker("nativelib" , "Native Library" ),
    ScalaNativeMaker("clib"      , "C Library"      ),
    ScalaNativeMaker("posixlib"  , "Posix Library"  ),
    ScalaNativeMaker("windowslib", "Windows Library"),
    ScalaNativeMaker("javalib"   , "Java Library"   ),
    ScalaNativeMaker("auxlib"    , "Aux Library"    )
  )

  override def linker: ScalaDependency.Maker = Maker(JvmBackend, "tools", "Build Tools, including Linker")
  override def testAdapter: ScalaDependency.Maker = Maker(JvmBackend, "test-runner", "Test Runner")
  override def testBridge: ScalaDependency.Maker = ScalaNativeMaker("test-interface", "SBT Test Interface")

  override def library(scalaVersion: ScalaVersion): ScalaDependency.Maker =
    if scalaVersion.isScala3
    then new ScalaNativeMaker("scala3lib", "Scala 3 Library"):
      override def isVersionCompound: Boolean = true
    else new ScalaNativeMaker("scalalib" , "Scala 2 Library"):
      override def isVersionCompound: Boolean = true
      override def scala2: Boolean = true

  override def compiler: ScalaDependency.Maker = Maker(
    scalaBackend = JvmBackend,
    artifact = "nscplugin",
    what = "Compiler Plugin",
    isScalaVersionFull = true
  )

  override def junit4Plugin: ScalaDependency.Maker = Maker(
    scalaBackend = JvmBackend,
    artifact = "junit-plugin",
    what = "JUnit4 Compiler Plugin for generating bootstrappers",
    isScalaVersionFull = true
  )

  override def additionalPluginDependencyRequirements: Array[DependencyRequirement] = Array.empty

  override def additionalImplementationDependencyRequirements(
    backendVersion: Version,
    scalaVersion: ScalaVersion
  ): Array[DependencyRequirement] = Array.empty
  
  // TODO if exclusions are needed - do it; if not - clean up in the Scala Native repository?
  // // Exclude cross published version dependencies leading to conflicts in Scala 3 vs 2.13
  // // When using Scala 3 exclude Scala 2.13 standard native libraries,
  // // when using Scala 2.13 exclude Scala 3 standard native libraries
  // // Use full name, Maven style published artifacts cannot use artifact/cross version for exclusion rules
  // nativeStandardLibraries.map { lib =>
  //   val scalaBinVersion =
  //     if (scalaVersion.value.startsWith("3.")) "2.13"
  //     else "3"
  //   ExclusionRule()
  //     .withOrganization(organization)
  //     .withName(s"${lib}_native${ScalaNativeCrossVersion.currentBinaryVersion}_${scalaBinVersion}")
