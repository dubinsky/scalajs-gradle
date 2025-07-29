package org.podval.tools.scalajs

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.podval.tools.build.{DependencyRequirement, PreVersion, ScalaBinaryVersion, ScalaLibrary, Version}
import org.podval.tools.node.NodeExtension
import org.podval.tools.nonjvm.NonJvmBackend
import org.podval.tools.test.framework.JUnit4ScalaJS
import NonJvmBackend.Dep

object ScalaJSBackend extends NonJvmBackend(
  name = "Scala.js",
  group = "org.scala-js",
  versionDefault = Version("1.19.0"),
  sourceRoot = "js",
  artifactSuffix = "sjs1",
  pluginDependenciesConfigurationName = "scalajs",
  areCompilerPluginsBuiltIntoScala3 = true,
  libraryScala3  = Dep("scalajs-library"          , "Library"                           , _.scala2.jvm),
  libraryScala2  = Dep("scalajs-library"          , "Library"                           , _.scala2.jvm), // same
  compilerPlugin = Dep("scalajs-compiler"         , "Compiler Plugin for Scala 2"       , _.scala2),
  junit4Plugin   = Dep("scalajs-junit-test-plugin", "JUnit4 Compiler Plugin for Scala 2", _.scala2),
  linker         = Dep("scalajs-linker"           , "Linker"                            , _.scala2),
  testAdapter    = Dep("scalajs-sbt-test-adapter" , "Test Adapter for Node.js"          , _.scala2),
  testBridge     = Dep("scalajs-test-bridge"      , "Test Bridge for Node.js"           , _.scala2.jvm.withDependencyRequirementVersionExact),
  pluginDependencies = Array(ScalaJSEnv.jsDomNode),
  withDefaultVersion = Array(ScalaJSEnv.dom),
  withBackendVersion = Array.empty
):
  override protected def linkTaskClass    : Class[ScalaJSLinkTask.Main] = classOf[ScalaJSLinkTask.Main]
  override protected def testLinkTaskClass: Class[ScalaJSLinkTask.Test] = classOf[ScalaJSLinkTask.Test]
  override protected def runTaskClass     : Class[ScalaJSRunTask .Main] = classOf[ScalaJSRunTask .Main]
  override protected def testTaskClass    : Class[ScalaJSRunTask .Test] = classOf[ScalaJSRunTask .Test]

  override protected def junit4: JUnit4ScalaJS.type = JUnit4ScalaJS

  override protected def versionExtractor(version: PreVersion): Version = version.simple
  
  override protected def versionComposer(
    scalaLibrary: ScalaLibrary,
    backendVersion: Version
  ): Version = backendVersion

  override protected def scalaCompileParameters(scalaLibrary: ScalaLibrary): Seq[String] =
    if scalaLibrary.isScala3
    then Seq("-scalajs")
    else Seq.empty
  
  // There is no Scala 2 equivalent.
  private def scala3Library: Dep = Dep(
    "scala3-library",
    "Scala 3 library in Scala.js.",
    _
      .withGroup(ScalaBinaryVersion.group)
      .withVersionDefault(ScalaBinaryVersion.Scala3.versionDefault)
      .scala3
  )

  override protected def implementation(scalaLibrary: ScalaLibrary): Array[DependencyRequirement] =
    if scalaLibrary.isScala3
    then Array(scala3Library(this).required(scalaLibrary.scalaVersion.version))
    else Array.empty

  override def apply(
    project: Project, 
    jvmPluginServices: JvmPluginServices,
    isRunningInIntelliJ: Boolean
  ): Unit =
    super.apply(project, jvmPluginServices, isRunningInIntelliJ)

    project.getExtensions.create("node", classOf[NodeExtension])
