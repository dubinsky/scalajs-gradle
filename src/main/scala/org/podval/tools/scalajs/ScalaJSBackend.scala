package org.podval.tools.scalajs

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.podval.tools.build.{DependencyRequirement, ScalaLibrary, Version}
import org.podval.tools.node.NodeExtension
import org.podval.tools.nonjvm.NonJvmBackend
import org.podval.tools.test.framework.JUnit4ScalaJS
import ScalaJSDependency.*

object ScalaJSBackend extends NonJvmBackend(
  name = "Scala.js",
  group = "org.scala-js",
  versionDefault = Version("1.20.1"),
  sourceRoot = "js",
  artifactSuffix = "sjs1",
  pluginDependenciesConfigurationName = "scalajs",
  areCompilerPluginsBuiltIntoScala3 = true,
  libraryScala3  = Library,
  libraryScala2  = Library,
  compilerPlugin = CompilerPlugin,
  junit4Plugin   = Junit4Plugin,
  linker         = Linker,
  testAdapter    = TestAdapter,
  testBridge     = TestBridge,
  pluginDependencies = Array(JsDomNode),
  withDefaultVersion = Array(Dom),
  withBackendVersion = Array.empty
):
  override def isJs    : Boolean = true
  override def isNative: Boolean = false

  override protected def junit4: JUnit4ScalaJS.type = JUnit4ScalaJS

  override protected def linkTaskClass    : Class[ScalaJSLinkTask.Main] = classOf[ScalaJSLinkTask.Main]
  override protected def testLinkTaskClass: Class[ScalaJSLinkTask.Test] = classOf[ScalaJSLinkTask.Test]
  override protected def runTaskClass     : Class[ScalaJSRunTask .Main] = classOf[ScalaJSRunTask .Main]
  override protected def testTaskClass    : Class[ScalaJSRunTask .Test] = classOf[ScalaJSRunTask .Test]
  
  override protected def scalaCompileParameters(scalaLibrary: ScalaLibrary): Seq[String] =
    if scalaLibrary.isScala3
    then Seq("-scalajs")
    else Seq.empty

  override protected def implementation(scalaLibrary: ScalaLibrary): Array[DependencyRequirement] =
    if scalaLibrary.isScala3
    then Array(Scala3Library(this).required(scalaLibrary.scalaVersion.version))
    else Array.empty

  override def apply(
    project: Project, 
    jvmPluginServices: JvmPluginServices,
    isRunningInIntelliJ: Boolean
  ): Unit =
    super.apply(project, jvmPluginServices, isRunningInIntelliJ)

    project.getExtensions.create("node", classOf[NodeExtension])
