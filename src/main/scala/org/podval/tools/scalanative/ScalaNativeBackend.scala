package org.podval.tools.scalanative

import org.podval.tools.build.{CompoundVersion, DependencyRequirement, PreVersion, ScalaLibrary, Version}
import org.podval.tools.nonjvm.NonJvmBackend
import org.podval.tools.test.framework.JUnit4ScalaNative
import NonJvmBackend.Dep

object ScalaNativeBackend extends NonJvmBackend(
  name = "Scala Native",
  group = "org.scala-native",
  versionDefault = Version("0.5.8"),
  sourceRoot = "native",
  artifactSuffix = "native0.5",
  pluginDependenciesConfigurationName = "scalanative",
  areCompilerPluginsBuiltIntoScala3 = false,
  libraryScala3  = Dep("scala3lib"     , "Scala 3 Library", _.scala3.withVersionCompound),
  libraryScala2  = Dep("scalalib"      , "Scala 2 Library", _.scala2.withVersionCompound),
  compilerPlugin = Dep("nscplugin"     , "Compiler Plugin"),
  junit4Plugin   = Dep("junit-plugin"  , "JUnit4 Compiler Plugin for generating bootstrappers"),
  linker         = Dep("tools"         , "Build Tools, including Linker"),
  testAdapter    = Dep("test-runner"   , "Test Runner"),
  testBridge     = Dep("test-interface", "SBT Test Interface"),
  pluginDependencies = Array.empty,
  withDefaultVersion = Array.empty,
  withBackendVersion = Array(
    Dep("nativelib" , "Native Library" ),
    Dep("clib"      , "C Library"      ),
    Dep("posixlib"  , "Posix Library"  ),
    Dep("windowslib", "Windows Library"),
    Dep("javalib"   , "Java Library"   ),
    Dep("auxlib"    , "Aux Library"    )
  )
):
  override def isJs    : Boolean = false
  override def isNative: Boolean = true

  override protected def linkTaskClass    : Class[ScalaNativeLinkTask.Main] = classOf[ScalaNativeLinkTask.Main]
  override protected def testLinkTaskClass: Class[ScalaNativeLinkTask.Test] = classOf[ScalaNativeLinkTask.Test]
  override protected def runTaskClass     : Class[ScalaNativeRunTask .Main] = classOf[ScalaNativeRunTask .Main]
  override protected def testTaskClass    : Class[ScalaNativeRunTask .Test] = classOf[ScalaNativeRunTask .Test]

  override protected def junit4: JUnit4ScalaNative.type = JUnit4ScalaNative

  override protected def versionExtractor(version: PreVersion): Version = version.compound.right
  
  override protected def versionComposer(
    scalaLibrary: ScalaLibrary,
    backendVersion: Version
  ): PreVersion = new CompoundVersion(
    scalaLibrary.scalaVersion.version,
    backendVersion
  )

  override protected def scalaCompileParameters(scalaLibrary: ScalaLibrary): Seq[String] =
    if scalaLibrary.scalaVersion.binaryVersion.isScala213
    then Seq("-Ytasty-reader")
    else Seq.empty
  
  override protected def implementation(scalaLibrary: ScalaLibrary): Array[DependencyRequirement] = Array.empty

  // // Exclude cross published version dependencies leading to conflicts in Scala 3 vs 2.13
  // // When using Scala 3 exclude Scala 2.13 standard native libraries,
  // // when using Scala 2.13 exclude Scala 3 standard native libraries
  // nativeStandardLibraries.map { lib =>
  //   val scalaBinVersion = if (scalaVersion.value.startsWith("3.")) "2.13" else "3"
  //   ExclusionRule()
  //     .withOrganization(organization)
  //     .withName(s"${lib}_native${ScalaNativeCrossVersion.currentBinaryVersion}_${scalaBinVersion}")
