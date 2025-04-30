package org.podval.tools.scalajsplugin.scalanative

import org.gradle.api.Project
import org.podval.tools.build.{ScalaBackendKind, ScalaDependency, ScalaPlatform, Version}
import org.podval.tools.scalajsplugin.nonjvm.{BackendLinkMainTask, BackendLinkTask, BackendLinkTestTask,
  BackendRunMainTask, BackendTestTask, NonJvmBackendDelegate}
import org.podval.tools.scalajsplugin.BackendDelegateKind
import org.podval.tools.scalanative.ScalaNative
import org.podval.tools.test.framework.JUnit4ScalaNative

object ScalaNativeDelegate  extends BackendDelegateKind(
  sourceRoot = "native",
  backendKind = ScalaBackendKind.Native,
  mk = ScalaNativeDelegate.apply
)

final class ScalaNativeDelegate(
  project: Project,
  isModeMixed: Boolean
) extends NonJvmBackendDelegate(
  project,
  isModeMixed
):
  override protected def kind: BackendDelegateKind = ScalaNativeDelegate
  override protected def gradleNamesSuffix: String = "Native"
  override protected def pluginDependenciesConfiguration: String = "scalanative"
  override protected def areCompilerPluginsBuiltIntoScala3: Boolean = false

  override protected def linkMainTaskClass: Class[? <: BackendLinkMainTask[?]] = classOf[ScalaNativeLinkMainTask]
  override protected def linkTestTaskClass: Class[? <: BackendLinkTestTask[?]] = classOf[ScalaNativeLinkTestTask]
  override protected def runMainTaskClass : Class[? <: BackendRunMainTask [?]] = classOf[ScalaNativeRunMainTask ]
  override protected def testTaskClass    : Class[? <: BackendTestTask    [?]] = classOf[ScalaNativeTestTask    ]

  override protected def createExtensions(): Unit = ()
  override protected def scalaCompileParameters(isScala3: Boolean): Seq[String] = Seq.empty
  override protected def backendVersionExtractor(version: Version): Version = version.compound.right
  
  override protected def backendVersionComposer(projectScalaVersion: Version, backendVersion: Version): Version =
    Version.Compound(projectScalaVersion.simple, backendVersion.simple)

  override protected def backendVersionDependency(isScala3: Boolean): ScalaDependency.Maker =
    if isScala3 then ScalaNative.Scala3Lib else ScalaNative.ScalaLib

  override protected def compilerScalaCompilerPluginDependency: ScalaDependency.Maker = ScalaNative.NSCPlugin
  override protected def linkerDependency: ScalaDependency.Maker = ScalaNative.Tools
  override protected def testAdapterDependency: ScalaDependency.Maker = ScalaNative.TestRunner
  override protected def testBridgeDependency: ScalaDependency.Maker = ScalaNative.TestInterface
  override protected def junit4dependency: ScalaDependency.Maker = JUnit4ScalaNative
  override protected def junit4ScalaCompilerPluginDependency: ScalaDependency.Maker = ScalaNative.JUnitPlugin

  override protected def additionalPluginDependencyRequirements: DependencyRequirements = Seq.empty

  override protected def implementationDependencyRequirements(
    backendVersion: Version,
    projectScalaPlatform: ScalaPlatform
  ): DependencyRequirements = Seq(
    ScalaNative.NativeLib ,
    ScalaNative.CLib      ,
    ScalaNative.PosixLib  ,
    ScalaNative.WindowsLib,
    ScalaNative.JavaLib   ,
    ScalaNative.AuxLib    
  ).map(_.required(backendVersion))


