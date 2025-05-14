package org.podval.tools.scalajsplugin.nonjvm

import org.gradle.api.artifacts.Configuration
import org.podval.tools.build.{DependencyRequirement, ScalaBackendKind, ScalaDependency, ScalaPlatform, Version}
import org.podval.tools.scalajsplugin.{BackendDelegate, BackendDependencyRequirements}

trait NonJvm[T <: NonJvmTask[?]] extends BackendDelegate[T]:
  final override def linkTaskClassOpt    : Option[Class[? <: T & NonJvmLinkTask.Main[?]]] = Some(linkTaskClass    )
  final override def testLinkTaskClassOpt: Option[Class[? <: T & NonJvmLinkTask.Test[?]]] = Some(testLinkTaskClass)

  def linkTaskClass    : Class[? <: T & NonJvmLinkTask.Main[?]]
  def testLinkTaskClass: Class[? <: T & NonJvmLinkTask.Test[?]]

  def pluginDependenciesConfigurationName: String
  def areCompilerPluginsBuiltIntoScala3: Boolean
  def versionExtractor(version: Version): Version
  def versionComposer(projectScalaVersion: Version, backendVersion: Version): Version
  def library(isScala3: Boolean): ScalaDependency.Maker
  def implementation: Seq[ScalaDependency.Maker]
  def compiler    : ScalaDependency.Maker
  def linker      : ScalaDependency.Maker
  def testAdapter : ScalaDependency.Maker
  def testBridge  : ScalaDependency.Maker
  def junit4Plugin: ScalaDependency.Maker
  def junit4      : ScalaDependency.Maker

  def additionalPluginDependencyRequirements: Seq[DependencyRequirement[ScalaPlatform]]

  def additionalImplementationDependencyRequirements(
    backendVersion: Version,
    scalaVersion: Version,
    isScala3: Boolean
  ): Seq[DependencyRequirement[ScalaPlatform]]

  override def backendKind: ScalaBackendKind.NonJvm

  final override def pluginDependenciesConfigurationNameOpt: Option[String] = Some(pluginDependenciesConfigurationName)

  final override def dependencyRequirements(
    implementationConfiguration: Configuration,
    testImplementationConfiguration: Configuration,
    projectScalaPlatform: ScalaPlatform
  ): BackendDependencyRequirements =
    val scalaVersion: Version = projectScalaPlatform.scalaVersion
    val isScala3: Boolean = projectScalaPlatform.version.isScala3
    val libraryDependency: ScalaDependency.Maker = library(isScala3)

    val backendVersion: Version = libraryDependency
      .findInConfiguration(projectScalaPlatform, implementationConfiguration)
      .map(_.version)
      .map(versionExtractor)
      .getOrElse(libraryDependency.versionDefault)

    // Add JUnit4 compiler plugin only when JUnit4 is in use, otherwise with Scala.js `testClasses` task throws
    //   "scala.reflect.internal.MissingRequirementError: object org.junit.Test in compiler mirror not found.";
    // somehow, `classes` task works fine, so there is no need, it seems, to create for a separate configuration
    //   `testScalaCompilerPlugins` (like Scala Native SBT plugin does).
    BackendDependencyRequirements(
      implementation =
        Seq(libraryDependency.required(versionComposer(scalaVersion, backendVersion))) ++
        implementation.map(_.required(backendVersion)) ++
        additionalImplementationDependencyRequirements(backendVersion, scalaVersion, isScala3),
      testImplementation =
        Seq(testBridge.required(backendVersion)),
      pluginDependencies =
        Seq(linker, testAdapter).map(_.required(backendVersion)) ++
        additionalPluginDependencyRequirements,
      scalaCompilerPlugins =
        (if areCompilerPluginsBuiltIntoScala3 && isScala3 then Seq.empty else Seq(compiler) ++ (
          if junit4.findInConfiguration(projectScalaPlatform, testImplementationConfiguration).isEmpty
          then Seq.empty
          else Seq(junit4Plugin)
        )).map(_.required(backendVersion))
    )
