package org.podval.tools.build.nonjvm

import org.gradle.api.artifacts.Configuration
import org.podval.tools.build.{BackendDependencyRequirements, Dependency, DependencyRequirement, ScalaBackend,
  ScalaDependency, ScalaVersion, Version}
import org.podval.tools.util.Scala212Collections.{arrayConcat, arrayMap}

trait NonJvmBackend extends ScalaBackend:
  final override def testsCanNotBeForked: Boolean = true

  final override def artifactSuffixOpt: Option[String] = Some(artifactSuffix)
  final override def archiveAppendixOpt: Option[String] = Some(sourceRoot)

  def artifactSuffix: String

  def versionDefault: Version

  def areCompilerPluginsBuiltIntoScala3: Boolean
  def versionExtractor(version: Version): Version
  def versionComposer(projectScalaVersion: ScalaVersion, backendVersion: Version): Version
  def implementation: Array[ScalaDependency.Maker]
  def library(isScala3: Boolean): ScalaDependency.Maker
  def compiler: ScalaDependency.Maker
  def linker: ScalaDependency.Maker
  def testAdapter: ScalaDependency.Maker
  def testBridge: ScalaDependency.Maker
  def junit4Plugin: ScalaDependency.Maker
  def junit4: Dependency.Maker
  def additionalPluginDependencyRequirements: Array[DependencyRequirement]

  def additionalImplementationDependencyRequirements(
    backendVersion: Version,
    scalaVersion: ScalaVersion
  ): Array[DependencyRequirement]

  final override def dependencyRequirements(
    implementationConfiguration: Configuration,
    testImplementationConfiguration: Configuration,
    scalaVersion: ScalaVersion
  ): BackendDependencyRequirements =
    val libraryDependency: ScalaDependency.Maker = library(scalaVersion.isScala3)

    val backendVersion: Version = libraryDependency
      .findInConfiguration(implementationConfiguration)
      .map(_.version)
      .map(versionExtractor)
      .getOrElse(libraryDependency.versionDefault)

    val addScalaCompilerPlugins: Boolean = !areCompilerPluginsBuiltIntoScala3 || !scalaVersion.isScala3
    
    // Add JUnit4 compiler plugin:
    // only when JUnit4 is in use, otherwise with Scala.js `testClasses` task throws
    //   "scala.reflect.internal.MissingRequirementError: object org.junit.Test in compiler mirror not found.";
    // only to a separate `testScalaCompilerPlugins` configuration to avoid the error when compiling main sources.
    BackendDependencyRequirements(
      implementation =
        arrayConcat(
          arrayConcat(
            Array(libraryDependency.required(versionComposer(scalaVersion, backendVersion))),
            arrayMap(implementation, _.required(backendVersion))
          ),
          additionalImplementationDependencyRequirements(backendVersion, scalaVersion)
        ),
      testRuntimeOnly =
        Array(testBridge.required(backendVersion)),
      pluginDependencies =
        arrayConcat(
          arrayMap(Array(linker, testAdapter), _.required(backendVersion)),
          additionalPluginDependencyRequirements
        ),
      scalaCompilerPlugins =
        if !addScalaCompilerPlugins
        then Array.empty
        else Array(compiler.required(backendVersion)),
      testScalaCompilerPlugins =
        if !addScalaCompilerPlugins || junit4.findInConfiguration(testImplementationConfiguration).isEmpty 
        then Array.empty
        else Array(junit4Plugin.required(backendVersion))
    )
