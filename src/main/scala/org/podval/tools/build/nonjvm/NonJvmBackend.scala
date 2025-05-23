package org.podval.tools.build.nonjvm

import org.gradle.api.artifacts.Configuration
import org.podval.tools.build.{BackendDependencyRequirements, DependencyRequirement, ScalaBackend, ScalaDependency, 
  ScalaPlatform, Version}
import org.podval.tools.util.Scala212Collections.{arrayConcat, arrayMap}

trait NonJvmBackend extends ScalaBackend:
  final override def testsCanNotBeForked: Boolean = true

  final override def artifactSuffixOpt: Option[String] = Some(artifactSuffix)
  final override def archiveAppendixOpt: Option[String] = Some(sourceRoot)

  def artifactSuffix: String

  def versionDefault: Version

  def areCompilerPluginsBuiltIntoScala3: Boolean
  def versionExtractor(version: Version): Version
  def versionComposer(projectScalaVersion: Version, backendVersion: Version): Version
  def implementation: Array[ScalaDependency.Maker]
  def library(isScala3: Boolean): ScalaDependency.Maker
  def compiler: ScalaDependency.Maker
  def linker: ScalaDependency.Maker
  def testAdapter: ScalaDependency.Maker
  def testBridge: ScalaDependency.Maker
  def junit4Plugin: ScalaDependency.Maker
  def junit4: ScalaDependency.Maker
  def additionalPluginDependencyRequirements: Array[DependencyRequirement[ScalaPlatform]]

  def additionalImplementationDependencyRequirements(
    backendVersion: Version,
    scalaVersion: Version,
    isScala3: Boolean
  ): Array[DependencyRequirement[ScalaPlatform]]

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
        arrayConcat(
          arrayConcat(
            Array(libraryDependency.required(versionComposer(scalaVersion, backendVersion))),
            arrayMap(implementation, _.required(backendVersion))
          ),
          additionalImplementationDependencyRequirements(backendVersion, scalaVersion, isScala3)
        ),
      testRuntimeOnly =
        Array(testBridge.required(backendVersion)),
      pluginDependencies =
        arrayConcat(
          arrayMap(Array(linker, testAdapter), _.required(backendVersion)),
          additionalPluginDependencyRequirements
        ),
      scalaCompilerPlugins =
        val result: Array[ScalaDependency.Maker] = 
          if areCompilerPluginsBuiltIntoScala3 && isScala3 then Array.empty else
            if junit4.findInConfiguration(projectScalaPlatform, testImplementationConfiguration).isEmpty
            then Array(compiler)
            else Array(compiler, junit4Plugin)
        arrayMap(result, _.required(backendVersion))  
    )
