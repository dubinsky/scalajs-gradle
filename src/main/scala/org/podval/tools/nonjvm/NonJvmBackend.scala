package org.podval.tools.nonjvm

import org.gradle.api.artifacts.Configuration
import org.podval.tools.build.{CreateExtension, DependencyMaker, DependencyRequirement, LinkTask, PreVersion, RunTask,
  ScalaBackend, ScalaDependencyMaker, ScalaVersion, SourceMapper, Version}
import org.podval.tools.test.framework.FrameworkDescriptor
import org.podval.tools.util.Scala212Collections.{arrayConcat, arrayMap}
import sbt.testing.Framework

abstract class NonJvmBackend(
  name: String,
  sourceRoot: String,
  artifactSuffix: String,
  pluginDependenciesConfigurationName: String,
  createExtension: Option[CreateExtension[?]]
) extends ScalaBackend(
  name = name,
  sourceRoot = sourceRoot,
  artifactSuffix = Some(artifactSuffix),
  archiveAppendix = Some(sourceRoot),
  testsCanNotBeForked = true,
  pluginDependenciesConfigurationName = Some(pluginDependenciesConfigurationName),
  createExtension = createExtension
):
  final override def linkTaskClassOpt    : Option[Class[? <: LinkTask.Main]] = Some(linkTaskClass)
  final override def testLinkTaskClassOpt: Option[Class[? <: LinkTask.Test]] = Some(testLinkTaskClass)
  final override def runTaskClassOpt     : Option[Class[? <: RunTask.Main & TaskWithLink[?]]] = Some(runTaskClass)
  override def testTaskClass             :        Class[? <: RunTask.Test & TaskWithLink[?]]

  def linkTaskClass    : Class[? <: LinkTask.Main]
  def testLinkTaskClass: Class[? <: LinkTask.Test]
  def runTaskClass     : Class[? <: RunTask.Main & TaskWithLink[?]]

  def areCompilerPluginsBuiltIntoScala3: Boolean
  def versionDefault: Version
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
    backendVersion: Version,
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
  ): ScalaBackend.DependencyRequirements =
    val backendVersion: Version = NonJvmBackend.this.backendVersion(
      scalaVersion, 
      implementationConfiguration
    )

    val addScalaCompilerPlugins: Boolean = !areCompilerPluginsBuiltIntoScala3 || !scalaVersion.isScala3
    
    // Add JUnit4 compiler plugin:
    // only when JUnit4 is in use, otherwise with Scala.js `testClasses` task throws
    //   "scala.reflect.internal.MissingRequirementError: object org.junit.Test in compiler mirror not found.";
    // only to a separate `testScalaCompilerPlugins` configuration to avoid the error when compiling main sources.
    ScalaBackend.DependencyRequirements(
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

  final def createTestEnvironment[A](
    testAdapter: A,
    loadFrameworksFromTestAdapter: (A, List[List[String]]) => List[Option[Framework]],
    closeTestAdapter: A => Unit,
    sourceMapperOpt: Option[SourceMapper]
  ): TestEnvironment = new TestEnvironment:
    override def sourceMapper: Option[SourceMapper] = sourceMapperOpt

    override protected def expandClassPath: Boolean = false
    
    override def close(): Unit = closeTestAdapter(testAdapter)

    override protected def loadFrameworks: List[Framework] =
      loadFrameworksFromTestAdapter(
        testAdapter,
        frameworksToLoad.map((descriptor: FrameworkDescriptor) => List(descriptor.className))
      ).flatten
