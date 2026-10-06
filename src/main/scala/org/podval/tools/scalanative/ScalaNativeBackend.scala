package org.podval.tools.scalanative

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.podval.tools.build.{DependencyRequirement, ScalaDependency, ScalaLibrary, ScalaBinaryVersion, Version}
import org.podval.tools.nonjvm.NonJvmBackend

object ScalaNativeBackend extends NonJvmBackend(
  name = "Scala Native",
  group = "org.scala-native",
  versionDefault = Version("0.5.12"),
  sourceRoot = "native",
  artifactSuffix = "native0.5",
  pluginDependenciesConfigurationName = "scalanative",
  areCompilerPluginsBuiltIntoScala3 = false
):
  override def apply(
    project: Project,
    jvmPluginServices: JvmPluginServices,
    isRunningInIntelliJ: Boolean
  ): Unit =
    super.apply(project, jvmPluginServices, isRunningInIntelliJ)
    ScalaNativeLinkTask.configureTasks(project)

  override protected def linkTaskClass    : Class[ScalaNativeLinkTask.Main] = classOf[ScalaNativeLinkTask.Main]
  override protected def testLinkTaskClass: Class[ScalaNativeLinkTask.Test] = classOf[ScalaNativeLinkTask.Test]
  override protected def runTaskClass     : Class[ScalaNativeRunTask .Main] = classOf[ScalaNativeRunTask .Main]
  override protected def testTaskClass    : Class[ScalaNativeRunTask .Test] = classOf[ScalaNativeRunTask .Test]

  override protected def compilerPlugin: ScalaDependency =
    scalaDependency(artifact = "nscplugin", what = "Compiler Plugin")
  
  override protected def junit4Plugin: ScalaDependency =
    scalaDependency(artifact = "junit-plugin", what = "JUnit4 Compiler Plugin for generating bootstrappers")
  
  override protected def linker: ScalaDependency =
    scalaDependency(artifact = "tools", what = "Build Tools, including Linker")
  
  override protected def testAdapter: ScalaDependency =
    scalaDependency(artifact = "test-runner", what = "Test Runner")
    
  override protected def testBridge: ScalaDependency =
    scalaDependency(artifact = "test-interface", what = "SBT Test Interface")

  override protected def library(scalaLibrary: ScalaLibrary): ScalaDependency = (
    scalaLibrary.scalaVersion.binaryVersion match
      case _: ScalaBinaryVersion.Scala3 =>
        scalaDependency(artifact = "scala3lib", what = "Scala 3 Library").scala3
      case _ =>
        scalaDependency(artifact = "scalalib" , what = "Scala 2 Library").scala2
  ).versionCompound

  override protected def pluginDependencies: Array[ScalaDependency] = Array.empty
  override protected def withDefaultVersion: Array[ScalaDependency] = Array.empty

  override protected def withBackendVersion: Array[ScalaDependency] =
    Array("Native", "C", "Posix", "Windows", "Java", "Aux")
      .map(name => scalaDependency(artifact = s"${name.toLowerCase}lib", what = s"$name Library"))

  override protected def junit4: JUnit4ScalaNative.type = JUnit4ScalaNative

  override protected def scalaCompileParameters(scalaLibrary: ScalaLibrary): Seq[String] =
    scalaLibrary.scalaVersion.binaryVersion match
      // Lets Scala 2.13 read TASTy published by Scala 3.7 and earlier.
      case ScalaBinaryVersion.Scala2_13 => Seq("-Ytasty-reader")
      case _ => Seq.empty
  
  override protected def implementation(scalaLibrary: ScalaLibrary): Array[DependencyRequirement] = Array.empty
