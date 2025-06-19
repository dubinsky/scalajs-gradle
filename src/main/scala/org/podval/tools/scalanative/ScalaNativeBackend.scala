package org.podval.tools.scalanative

import org.podval.tools.build.{BackendTask, CompoundVersion, DependencyMaker, DependencyRequirement, PreVersion,
  ScalaBackend, ScalaBinaryVersion, ScalaDependencyMaker, ScalaVersion, Version}
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.nonjvm.NonJvmBackend
import org.podval.tools.test.framework.JUnit4ScalaNative

case object ScalaNativeBackend extends NonJvmBackend(
  name = "Scala Native",
  sourceRoot = "native",
  artifactSuffix = "native0.5",
  pluginDependenciesConfigurationName = "scalanative",
  createExtension = None
):
  override val versionDefault: Version = Version("0.5.8")

  override def linkTaskClass    : Class[ScalaNativeLinkTask.Main] = classOf[ScalaNativeLinkTask.Main]
  override def testLinkTaskClass: Class[ScalaNativeLinkTask.Test] = classOf[ScalaNativeLinkTask.Test]
  override def runTaskClass     : Class[ScalaNativeRunTask .Main] = classOf[ScalaNativeRunTask .Main]
  override def testTaskClass    : Class[ScalaNativeRunTask .Test] = classOf[ScalaNativeRunTask .Test]

  override def scalaCompileParameters(scalaVersion: ScalaVersion): Seq[String] =
    if scalaVersion.binaryVersion == ScalaBinaryVersion.Scala213
    then Seq("-Ytasty-reader")
    else Seq.empty
    
  override def areCompilerPluginsBuiltIntoScala3: Boolean = false
  override def junit4: DependencyMaker = JUnit4ScalaNative.forNative.get.maker
  override def versionExtractor(version: PreVersion): Version = version.compound.right
  
  override def versionComposer(
    projectScalaVersion: ScalaVersion,
    backendVersion: Version
  ): PreVersion = new CompoundVersion(
    projectScalaVersion.version,
    backendVersion
  )

  private val group: String = "org.scala-native"

  private sealed class Maker(
    final override val scalaBackend: ScalaBackend,
    final override val artifact: String,
    what: String,
    final override val isScalaVersionFull: Boolean = false
  ) extends ScalaDependencyMaker:
    final override def description: String = describe(what)
    final override def versionDefault: Version = ScalaNativeBackend.versionDefault
    final override def group: String = ScalaNativeBackend.group

  private sealed class ScalaNativeMaker(
    artifact: String,
    what: String
  ) extends Maker(
    ScalaNativeBackend,
    artifact, 
    what
  )
  
  override def implementation: Array[ScalaDependencyMaker] = Array(
    ScalaNativeMaker("nativelib" , "Native Library" ),
    ScalaNativeMaker("clib"      , "C Library"      ),
    ScalaNativeMaker("posixlib"  , "Posix Library"  ),
    ScalaNativeMaker("windowslib", "Windows Library"),
    ScalaNativeMaker("javalib"   , "Java Library"   ),
    ScalaNativeMaker("auxlib"    , "Aux Library"    )
  )

  override def linker: ScalaDependencyMaker = Maker(JvmBackend, "tools", "Build Tools, including Linker")
  override def testAdapter: ScalaDependencyMaker = Maker(JvmBackend, "test-runner", "Test Runner")
  override def testBridge: ScalaDependencyMaker = ScalaNativeMaker("test-interface", "SBT Test Interface")

  override def library(scalaVersion: ScalaVersion): ScalaDependencyMaker =
    if scalaVersion.isScala3
    then new ScalaNativeMaker("scala3lib", "Scala 3 Library"):
      override def isVersionCompound: Boolean = true
    else new ScalaNativeMaker("scalalib" , "Scala 2 Library"):
      override def isVersionCompound: Boolean = true
      override def isPublishedForScala3: Boolean = false

  override def compiler: ScalaDependencyMaker = Maker(
    scalaBackend = JvmBackend,
    artifact = "nscplugin",
    what = "Compiler Plugin",
    isScalaVersionFull = true
  )

  override def junit4Plugin: ScalaDependencyMaker = Maker(
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
