package org.podval.tools.jvm

import org.gradle.api.artifacts.Configuration
import org.podval.tools.build.{LinkTask, RunTask, ScalaBackend, ScalaVersion, SourceMapper, Version}
import org.podval.tools.test.framework.{FrameworkDescriptor, FrameworkProvider}
import sbt.testing.Framework

case object JvmBackend extends ScalaBackend(
  name = "JVM",
  sourceRoot = "jvm",
  artifactSuffix = None,
  archiveAppendix = None,
  testsCanNotBeForked = false,
  pluginDependenciesConfigurationName = None,
  createExtension = None
):
  override def linkTaskClassOpt    : Option[Class[? <: LinkTask.Main]] = None
  override def testLinkTaskClassOpt: Option[Class[? <: LinkTask.Test]] = None
  override def runTaskClassOpt     : Option[Class[? <: RunTask .Main]] = None
  override def testTaskClass       :        Class[? <: RunTask .Test]  = classOf[JvmTestTask]

  override def scalaCompileParameters(scalaVersion: ScalaVersion): Seq[String] = Seq.empty

  override def dependencyRequirements(
    implementationConfiguration: Configuration,
    testImplementationConfiguration: Configuration,
    scalaVersion: ScalaVersion
  ): ScalaBackend.DependencyRequirements = ScalaBackend.DependencyRequirements(
    implementation = Array.empty,
    testRuntimeOnly = Array(SbtTestInterface.required()),
    scalaCompilerPlugins = Array.empty,
    testScalaCompilerPlugins = Array.empty,
    pluginDependencies = Array.empty
  )
  
  def createTestEnvironment: TestEnvironment = new TestEnvironment:
    override def expandClassPath: Boolean = true
    override def sourceMapper: Option[SourceMapper] = None
    override def close(): Unit = ()
    override protected def loadFrameworks: List[Framework] = frameworksToLoad.flatMap(FrameworkProvider(_).frameworkOpt)
