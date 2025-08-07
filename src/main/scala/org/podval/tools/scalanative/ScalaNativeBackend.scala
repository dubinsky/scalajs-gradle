package org.podval.tools.scalanative

import org.podval.tools.build.{DependencyRequirement, ScalaLibrary, Version}
import org.podval.tools.nonjvm.NonJvmBackend
import org.podval.tools.test.framework.JUnit4ScalaNative
import ScalaNativeDependency.*

object ScalaNativeBackend extends NonJvmBackend(
  name = "Scala Native",
  group = "org.scala-native",
  versionDefault = Version("0.5.8"),
  sourceRoot = "native",
  artifactSuffix = "native0.5",
  pluginDependenciesConfigurationName = "scalanative",
  areCompilerPluginsBuiltIntoScala3 = false,
  libraryScala3  = LibraryScala3,
  libraryScala2  = LibraryScala2,
  compilerPlugin = CompilerPlugin,
  junit4Plugin   = Junit4Plugin,
  linker         = Linker,
  testAdapter    = TestAdapter,
  testBridge     = TestBridge,
  pluginDependencies = Array.empty,
  withDefaultVersion = Array.empty,
  withBackendVersion = Array(NativeLib, CLib, PosixLib, WindowsLib, JavaLib, AuxLib)
):
  override def isJs    : Boolean = false
  override def isNative: Boolean = true

  override protected def junit4: JUnit4ScalaNative.type = JUnit4ScalaNative

  override protected def linkTaskClass    : Class[ScalaNativeLinkTask.Main] = classOf[ScalaNativeLinkTask.Main]
  override protected def testLinkTaskClass: Class[ScalaNativeLinkTask.Test] = classOf[ScalaNativeLinkTask.Test]
  override protected def runTaskClass     : Class[ScalaNativeRunTask .Main] = classOf[ScalaNativeRunTask .Main]
  override protected def testTaskClass    : Class[ScalaNativeRunTask .Test] = classOf[ScalaNativeRunTask .Test]

  override protected def scalaCompileParameters(scalaLibrary: ScalaLibrary): Seq[String] =
    if scalaLibrary.scalaVersion.isScala213
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
