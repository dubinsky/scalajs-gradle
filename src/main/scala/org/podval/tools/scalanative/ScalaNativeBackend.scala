package org.podval.tools.scalanative

import org.podval.tools.build.{DependencyRequirement, ScalaDependency, ScalaLibrary, ScalaBinaryVersion, Version}
import org.podval.tools.nonjvm.NonJvmBackend
import org.podval.tools.test.framework.JUnit4ScalaNative

object ScalaNativeBackend extends NonJvmBackend(
  name = "Scala Native",
  group = "org.scala-native",
  versionDefault = Version("0.5.10"),
  sourceRoot = "native",
  artifactSuffix = "native0.5",
  pluginDependenciesConfigurationName = "scalanative",
  areCompilerPluginsBuiltIntoScala3 = false
):
  override protected def linkTaskClass    : Class[ScalaNativeLinkTask.Main] = classOf[ScalaNativeLinkTask.Main]
  override protected def testLinkTaskClass: Class[ScalaNativeLinkTask.Test] = classOf[ScalaNativeLinkTask.Test]
  override protected def runTaskClass     : Class[ScalaNativeRunTask .Main] = classOf[ScalaNativeRunTask .Main]
  override protected def testTaskClass    : Class[ScalaNativeRunTask .Test] = classOf[ScalaNativeRunTask .Test]

  override protected def compilerPlugin: ScalaDependency =
    scalaDependency("nscplugin", "Compiler Plugin")
  override protected def junit4Plugin: ScalaDependency =
    scalaDependency("junit-plugin", "JUnit4 Compiler Plugin for generating bootstrappers")
  override protected def linker: ScalaDependency =
    scalaDependency("tools", "Build Tools, including Linker")
  override protected def testAdapter: ScalaDependency =
    scalaDependency("test-runner", "Test Runner")
  override protected def testBridge: ScalaDependency =
    scalaDependency("test-interface", "SBT Test Interface")

  override protected def library(scalaLibrary: ScalaLibrary): ScalaDependency = (
    scalaLibrary.scalaVersion.binaryVersion match
      case _: ScalaBinaryVersion.Scala3 => scalaDependency("scala3lib", "Scala 3 Library").scala3
      case _                      => scalaDependency("scalalib" , "Scala 2 Library").scala2
  ).withVersionCompound

  override protected def pluginDependencies: Array[ScalaDependency] = Array.empty
  override protected def withDefaultVersion: Array[ScalaDependency] = Array.empty

  override protected def withBackendVersion: Array[ScalaDependency] = Array(
    scalaDependency("nativelib" , "Native Library" ),
    scalaDependency("clib"      , "C Library"      ),
    scalaDependency("posixlib"  , "Posix Library"  ),
    scalaDependency("windowslib", "Windows Library"),
    scalaDependency("javalib"   , "Java Library"   ),
    scalaDependency("auxlib"    , "Aux Library"    )
  )

  override protected def junit4: JUnit4ScalaNative.type = JUnit4ScalaNative

  override protected def scalaCompileParameters(scalaLibrary: ScalaLibrary): Seq[String] =
    scalaLibrary.scalaVersion.binaryVersion match
      case ScalaBinaryVersion.Scala2_13 => Seq("-Ytasty-reader")
      case _ => Seq.empty
  
  override protected def implementation(scalaLibrary: ScalaLibrary): Array[DependencyRequirement] = Array.empty

  // // Exclude cross published version dependencies leading to conflicts in Scala 3 vs 2.13
  // // When using Scala 3 exclude Scala 2.13 standard native libraries,
  // // when using Scala 2.13 exclude Scala 3 standard native libraries
  // nativeStandardLibraries.map { lib =>
  //   val scalaBinVersion = if (scalaVersion.value.startsWith("3.")) "2.13" else "3"
  //   ExclusionRule()
  //     .withOrganization(organization)
  //     .withName(s"${lib}_native${ScalaNativeCrossVersion.currentBinaryVersion}_${scalaBinVersion}")
