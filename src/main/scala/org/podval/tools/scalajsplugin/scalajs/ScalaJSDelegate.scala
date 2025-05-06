package org.podval.tools.scalajsplugin.scalajs

import org.gradle.api.Project
import org.podval.tools.build.{ScalaBackendKind, ScalaDependency, ScalaPlatform, ScalaVersion, Version}
import org.podval.tools.node.NodeExtension
import org.podval.tools.scalajs.ScalaJS
import org.podval.tools.scalajsplugin.nonjvm.{NonJvmLinkMainTask, NonJvmLinkTask, NonJvmLinkTestTask,
  NonJvmRunMainTask, NonJvmTestTask, NonJvmDelegate}
import org.podval.tools.scalajsplugin.BackendDelegateKind
import org.podval.tools.test.framework.JUnit4ScalaJS
import scala.jdk.CollectionConverters.SeqHasAsJava

object ScalaJSDelegate  extends BackendDelegateKind(
  sourceRoot = "js",
  backendKind = ScalaBackendKind.JS,
  mk = ScalaJSDelegate.apply
)

final class ScalaJSDelegate(
  project: Project,
  isModeNixed: Boolean
) extends NonJvmDelegate(
  project,
  isModeNixed
):
  override protected def kind: BackendDelegateKind = ScalaJSDelegate
  override protected def gradleNamesSuffix: String = "JS"
  override protected def pluginDependenciesConfigurationName: String = "scalajs"
  override protected def areCompilerPluginsBuiltIntoScala3: Boolean = true

  override protected def linkMainTaskClass: Class[? <: NonJvmLinkMainTask   [?]] = classOf[ScalaJSLinkMainTask]
  override protected def linkTestTaskClass: Class[? <: NonJvmLinkTestTask   [?]] = classOf[ScalaJSLinkTestTask]
  override protected def runMainTaskClass : Class[? <: NonJvmRunMainTask    [?]] = classOf[ScalaJSRunMainTask ]
  override protected def testTaskClass    : Class[? <: NonJvmTestTask       [?]] = classOf[ScalaJSTestTask    ]

  override protected def createExtensions(): Unit =
    val nodeExtension: NodeExtension = NodeExtension.addTo(project)
    nodeExtension.getModules.convention(List("jsdom").asJava)

  override protected def scalaCompileParameters(isScala3: Boolean): Seq[String] =
    if !isScala3 then Seq.empty else Seq("-scalajs")
  
  override protected def backendVersionExtractor(version: Version): Version =
    version

  override protected def backendVersionComposer(projectScalaVersion: Version, backendVersion: Version): Version =
    backendVersion.simple

  override protected def backendVersionDependency(isScala3: Boolean): ScalaDependency.Maker = ScalaJS.Library
  override protected def compilerScalaCompilerPluginDependency: ScalaDependency.Maker = ScalaJS.Compiler
  override protected def linkerDependency: ScalaDependency.Maker = ScalaJS.Linker
  override protected def testAdapterDependency: ScalaDependency.Maker = ScalaJS.TestAdapter
  override protected def testBridgeDependency: ScalaDependency.Maker = ScalaJS.TestBridge
  override protected def junit4dependency: ScalaDependency.Maker = JUnit4ScalaJS
  override protected def junit4ScalaCompilerPluginDependency: ScalaDependency.Maker = ScalaJS.JUnitPlugin

  override protected def additionalPluginDependencyRequirements: DependencyRequirements = Seq(
    // ScalaModules.ParallelCollections,
    ScalaJS.JSDomNodeJS.required()
  )

  override protected def implementationDependencyRequirements(
    backendVersion: Version,
    projectScalaPlatform: ScalaPlatform
  ): DependencyRequirements = Seq(
    ScalaJS.DomSJS.required()
  ) ++
  // only for Scala 3
  (if !projectScalaPlatform.version.isScala3 then Seq.empty else Seq(
    ScalaVersion.Scala3.ScalaLibraryJS.required(projectScalaPlatform.scalaVersion)
  ))
