package org.podval.tools.scalanative

import org.podval.tools.build.{ScalaBackendKind, ScalaDependency, Version}

object ScalaNative:
  val group: String = "org.scala-native"

  val versionDefault: Version = Version("0.5.7")
  
  sealed class Maker(
    final override val artifact: String,
    what: String,
    final override val isScalaVersionFull: Boolean = false
  ) extends ScalaDependency.Maker:    
    final override def description: String = s"Scala Native $what."
    final override def versionDefault: Version = ScalaNative.versionDefault
    final override def group: String = ScalaNative.group

  // Compiler compiler plugins
  object NSCPlugin extends Maker(
    "nscplugin",
    "Compiler Plugin for compiling Scala Native code",
    isScalaVersionFull = true
  ) with ScalaDependency.MakerJvm

  // JUnit4 compiler plugin
  object JUnitPlugin extends Maker(
    "junit-plugin",
    "JUnit4 Compiler Plugin for generating JUnit4 bootstrappers",
    isScalaVersionFull = true
  ) with ScalaDependency.MakerJvm

  object NativeLib     extends Maker("nativelib"     , "Native Library")
  object CLib          extends Maker("clib"          , "C Library")
  object PosixLib      extends Maker("posixlib"      , "Posix Library")
  object WindowsLib    extends Maker("windowslib"    , "Windows Library")
  object JavaLib       extends Maker("javalib"       , "Java Library")
  object AuxLib        extends Maker("auxlib"        , "Aux Library")
  object ScalaLib      extends Maker("scalalib"      , "Scala 2 Library") with ScalaDependency.MakerScala2
  object Scala3Lib     extends Maker("scala3lib"     , "Scala 3 Library")
  object Tools         extends Maker("tools"         , "Build Tools, including Linker for linking Scala Native code")
  object TestRunner    extends Maker("test-runner"   , "Test Runner") with ScalaDependency.MakerJvm
  object TestInterface extends Maker("test-interface", "SBT Test Interface")

// TODO exclusions?
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
