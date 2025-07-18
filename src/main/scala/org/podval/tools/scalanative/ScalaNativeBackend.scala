package org.podval.tools.scalanative

import org.podval.tools.build.{CompoundVersion, DependencyMaker, DependencyRequirement, PreVersion, ScalaBinaryVersion,
  ScalaDependencyMaker, ScalaVersion, Version}
import org.podval.tools.nonjvm.NonJvmBackend
import org.podval.tools.test.framework.JUnit4ScalaNative

object ScalaNativeBackend extends NonJvmBackend(
  name = "Scala Native",
  group = "org.scala-native",
  versionDefault = Version("0.5.8"),
  sourceRoot = "native",
  artifactSuffix = "native0.5",
  pluginDependenciesConfigurationName = "scalanative",
  areCompilerPluginsBuiltIntoScala3 = false
):
  override protected def linkTaskClass    : Class[ScalaNativeLinkTask.Main] = classOf[ScalaNativeLinkTask.Main]
  override protected def testLinkTaskClass: Class[ScalaNativeLinkTask.Test] = classOf[ScalaNativeLinkTask.Test]
  override protected def runTaskClass     : Class[ScalaNativeRunTask .Main] = classOf[ScalaNativeRunTask .Main]
  override protected def testTaskClass    : Class[ScalaNativeRunTask .Test] = classOf[ScalaNativeRunTask .Test]

  override protected def scalaCompileParameters(scalaVersion: ScalaVersion): Seq[String] =
    if scalaVersion.binaryVersion == ScalaBinaryVersion.Scala2.P13
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

  override protected def junit4: JUnit4ScalaNative.type = JUnit4ScalaNative

  override protected def library(isScala3: Boolean): BackendDependency =
    if isScala3 then new BackendDependency("scala3lib", "Scala 3 Library")
      with DependencyMaker.IsVersionCompound
      with ScalaDependencyMaker.Scala3
    else new BackendDependency("scalalib", "Scala 2 Library")
      with DependencyMaker.IsVersionCompound
      with ScalaDependencyMaker.Scala2

  override protected def compilerPlugin: Plugin = new Jvm("nscplugin", "Compiler Plugin") with Plugin
  override protected def junit4Plugin: Plugin = new Jvm("junit-plugin", "JUnit4 Compiler Plugin for generating bootstrappers") with Plugin
  override protected def linker: Jvm = Jvm("tools", "Build Tools, including Linker")
  override protected def testAdapter: Jvm = Jvm("test-runner", "Test Runner")
  override protected def testBridge: BackendDependency = BackendDependency("test-interface", "SBT Test Interface")

  override protected def pluginDependencies: Array[Jvm] = Array.empty
  override protected def implementationDependencyRequirements(scalaVersion: ScalaVersion): Array[DependencyRequirement] = Array.empty
  override def implementation: Array[BackendDependency] = Array(
    BackendDependency("nativelib" , "Native Library" ),
    BackendDependency("clib"      , "C Library"      ),
    BackendDependency("posixlib"  , "Posix Library"  ),
    BackendDependency("windowslib", "Windows Library"),
    BackendDependency("javalib"   , "Java Library"   ),
    BackendDependency("auxlib"    , "Aux Library"    )
  )

  // // Exclude cross published version dependencies leading to conflicts in Scala 3 vs 2.13
  // // When using Scala 3 exclude Scala 2.13 standard native libraries,
  // // when using Scala 2.13 exclude Scala 3 standard native libraries
  // nativeStandardLibraries.map { lib =>
  //   val scalaBinVersion = if (scalaVersion.value.startsWith("3.")) "2.13" else "3"
  //   ExclusionRule()
  //     .withOrganization(organization)
  //     .withName(s"${lib}_native${ScalaNativeCrossVersion.currentBinaryVersion}_${scalaBinVersion}")
