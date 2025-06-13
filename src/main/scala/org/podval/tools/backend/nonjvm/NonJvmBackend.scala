package org.podval.tools.backend.nonjvm

import org.gradle.api.artifacts.Configuration
import org.podval.tools.build.{BackendDependencyRequirements, DependencyMaker, DependencyRequirement, PreVersion,
  ScalaBackend, ScalaDependencyMaker, ScalaVersion, Version}
import org.podval.tools.util.Scala212Collections.{arrayConcat, arrayMap}

trait NonJvmBackend extends ScalaBackend:
  final override def testsCanNotBeForked: Boolean = true

  final override def artifactSuffixOpt: Option[String] = Some(artifactSuffix)
  final override def archiveAppendixOpt: Option[String] = Some(sourceRoot)

  def artifactSuffix: String

  def versionDefault: PreVersion

  def areCompilerPluginsBuiltIntoScala3: Boolean
  def versionExtractor(version: PreVersion): Version
  def versionComposer(projectScalaVersion: ScalaVersion, backendVersion: Version): PreVersion
  def implementation: Array[ScalaDependencyMaker]
  def library(scalaVersion: ScalaVersion): ScalaDependencyMaker
  def compiler: ScalaDependencyMaker
  def linker: ScalaDependencyMaker
  def testAdapter: ScalaDependencyMaker
  def testBridge: ScalaDependencyMaker
  def junit4Plugin: ScalaDependencyMaker
  def junit4: DependencyMaker
  def additionalPluginDependencyRequirements: Array[DependencyRequirement]

  def additionalImplementationDependencyRequirements(
    backendVersion: PreVersion,
    scalaVersion: ScalaVersion
  ): Array[DependencyRequirement]

  final def backendVersion(
    scalaVersion: ScalaVersion,
    implementationConfiguration: Configuration
  ): Version =
    val libraryDependency: ScalaDependencyMaker = library(scalaVersion)
    libraryDependency
      .findable
      .findInConfiguration(implementationConfiguration)
      .map(_.version)
      .map(versionExtractor)
      .getOrElse(libraryDependency.versionDefault)

  final def junit4present(
    testImplementationConfiguration: Configuration
  ): Boolean = junit4
    .findable
    .findInConfiguration(testImplementationConfiguration).isDefined
    
  final override def dependencyRequirements(
    implementationConfiguration: Configuration,
    testImplementationConfiguration: Configuration,
    scalaVersion: ScalaVersion
  ): BackendDependencyRequirements =
    val backendVersion: Version = NonJvmBackend.this.backendVersion(
      scalaVersion, 
      implementationConfiguration
    )

    val addScalaCompilerPlugins: Boolean = !areCompilerPluginsBuiltIntoScala3 || !scalaVersion.isScala3
    
    // Add JUnit4 compiler plugin:
    // only when JUnit4 is in use, otherwise with Scala.js `testClasses` task throws
    //   "scala.reflect.internal.MissingRequirementError: object org.junit.Test in compiler mirror not found.";
    // only to a separate `testScalaCompilerPlugins` configuration to avoid the error when compiling main sources.
    BackendDependencyRequirements(
      implementation =
        arrayConcat(
          arrayConcat(
            Array(library(scalaVersion).required(versionComposer(scalaVersion, backendVersion))),
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
        if !addScalaCompilerPlugins || !junit4present(testImplementationConfiguration) 
        then Array.empty
        else Array(junit4Plugin.required(backendVersion))
    )
