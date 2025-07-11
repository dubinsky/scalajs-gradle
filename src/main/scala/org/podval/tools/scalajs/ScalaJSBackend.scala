package org.podval.tools.scalajs

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.podval.tools.build.{DependencyRequirement, PreVersion, ScalaVersion, Version}
import org.podval.tools.node.NodeExtension
import org.podval.tools.nonjvm.NonJvmBackend
import org.podval.tools.test.framework.JUnit4ScalaJS

object ScalaJSBackend extends NonJvmBackend(
  name = "Scala.js",
  sourceRoot = "js",
  artifactSuffix = "sjs1",
  pluginDependenciesConfigurationName = "scalajs",
  areCompilerPluginsBuiltIntoScala3 = true,
  junit4 = JUnit4ScalaJS,
  libraryScala3 = ScalaJSDependency.Library,
  libraryScala2 = ScalaJSDependency.Library,
  compiler      = ScalaJSDependency.Compiler,
  linker        = ScalaJSDependency.Linker,
  testAdapter   = ScalaJSDependency.TestAdapter,
  testBridge    = ScalaJSDependency.TestBridge,
  junit4Plugin  = ScalaJSDependency.JUnit4Plugin,
  pluginDependencies = Array(ScalaJSDependency.JSDomNodeJSEnv),
  implementation = Array.empty
):
  override protected def linkTaskClass    : Class[ScalaJSLinkTask.Main] = classOf[ScalaJSLinkTask.Main]
  override protected def testLinkTaskClass: Class[ScalaJSLinkTask.Test] = classOf[ScalaJSLinkTask.Test]
  override protected def runTaskClass     : Class[ScalaJSRunTask .Main] = classOf[ScalaJSRunTask .Main]
  override protected def testTaskClass    : Class[ScalaJSRunTask .Test] = classOf[ScalaJSRunTask .Test]

  override protected def scalaCompileParameters(scalaVersion: ScalaVersion): Seq[String] =
    if scalaVersion.isScala3 
    then Seq("-scalajs")
    else Seq.empty
    
  override protected def versionExtractor(version: PreVersion): Version = version.simple
  
  override protected def versionComposer(
    projectScalaVersion: ScalaVersion,
    backendVersion: Version
  ): Version = backendVersion
  
  override protected def implementationDependencyRequirements(scalaVersion: ScalaVersion): Array[DependencyRequirement] =
    if !scalaVersion.isScala3 // ++ is from IterableOnce and thus is not available on Scala 2.12...
    then Array(ScalaJSDependency.DomSJS.required())
    else Array(ScalaJSDependency.DomSJS.required(), ScalaJSDependency.Scala3LibraryJS.required(scalaVersion.version))

  override def apply(project: Project, jvmPluginServices: JvmPluginServices): Unit =
    super.apply(project, jvmPluginServices)

    project.getExtensions.create("node", classOf[NodeExtension])
