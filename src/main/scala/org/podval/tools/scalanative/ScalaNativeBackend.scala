package org.podval.tools.scalanative

import org.podval.tools.build.{CompoundVersion, DependencyRequirement, PreVersion, ScalaBinaryVersion, ScalaVersion,
  Version}
import org.podval.tools.nonjvm.NonJvmBackend
import org.podval.tools.test.framework.JUnit4ScalaNative

object ScalaNativeBackend extends NonJvmBackend(
  name = "Scala Native",
  sourceRoot = "native",
  artifactSuffix = "native0.5",
  pluginDependenciesConfigurationName = "scalanative",
  areCompilerPluginsBuiltIntoScala3 = false,
  junit4 = JUnit4ScalaNative,
  versionDefault = Version("0.5.8"),
  libraryScala3 = ScalaNativeDependency.Scala3Lib,
  libraryScala2 = ScalaNativeDependency.ScalaLib,
  compiler      = ScalaNativeDependency.Compiler,
  linker        = ScalaNativeDependency.Linker,
  testAdapter   = ScalaNativeDependency.TestAdapter,
  testBridge    = ScalaNativeDependency.TestBridge,
  junit4Plugin  = ScalaNativeDependency.JUnit4Plugin,
  pluginDependencies = Array.empty,
  implementation = Array(
    ScalaNativeDependency.NativeLib,
    ScalaNativeDependency.CLib,
    ScalaNativeDependency.PosixLib,
    ScalaNativeDependency.WindowsLib,
    ScalaNativeDependency.JavaLib,
    ScalaNativeDependency.AuxLib
  )
):
  override protected def linkTaskClass    : Class[ScalaNativeLinkTask.Main] = classOf[ScalaNativeLinkTask.Main]
  override protected def testLinkTaskClass: Class[ScalaNativeLinkTask.Test] = classOf[ScalaNativeLinkTask.Test]
  override protected def runTaskClass     : Class[ScalaNativeRunTask .Main] = classOf[ScalaNativeRunTask .Main]
  override protected def testTaskClass    : Class[ScalaNativeRunTask .Test] = classOf[ScalaNativeRunTask .Test]

  override protected def scalaCompileParameters(scalaVersion: ScalaVersion): Seq[String] =
    if scalaVersion.binaryVersion == ScalaBinaryVersion.Scala213
    then Seq("-Ytasty-reader")
    else Seq.empty
    
  override protected def versionExtractor(version: PreVersion): Version = version.compound.right
  
  override protected def versionComposer(
    projectScalaVersion: ScalaVersion,
    backendVersion: Version
  ): PreVersion = new CompoundVersion(
    projectScalaVersion.version,
    backendVersion
  )
  
  override protected def implementationDependencyRequirements(scalaVersion: ScalaVersion): Array[DependencyRequirement] =
    Array.empty
  
  // // Exclude cross published version dependencies leading to conflicts in Scala 3 vs 2.13
  // // When using Scala 3 exclude Scala 2.13 standard native libraries,
  // // when using Scala 2.13 exclude Scala 3 standard native libraries
  // nativeStandardLibraries.map { lib =>
  //   val scalaBinVersion = if (scalaVersion.value.startsWith("3.")) "2.13" else "3"
  //   ExclusionRule()
  //     .withOrganization(organization)
  //     .withName(s"${lib}_native${ScalaNativeCrossVersion.currentBinaryVersion}_${scalaBinVersion}")
