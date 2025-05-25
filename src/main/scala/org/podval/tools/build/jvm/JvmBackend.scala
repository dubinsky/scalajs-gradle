package org.podval.tools.build.jvm

import org.gradle.api.artifacts.Configuration
import org.podval.tools.build.{BackendDependencyRequirements, JavaDependency, ScalaBackend, ScalaVersion, Version}

case object JvmBackend extends ScalaBackend:
  override val name: String = "JVM"
  override val sourceRoot: String = "jvm"
  override val artifactSuffixOpt: Option[String] = None
  override val archiveAppendixOpt: Option[String] = None
  override val testsCanNotBeForked: Boolean = false

  override def scalaCompileParameters(scalaVersion: ScalaVersion): Seq[String] = Seq.empty

  override def dependencyRequirements(
    implementationConfiguration: Configuration,
    testImplementationConfiguration: Configuration,
    scalaVersion: ScalaVersion
  ): BackendDependencyRequirements = BackendDependencyRequirements(
    implementation = Array.empty,
    testRuntimeOnly = Array(SbtTestInterface.required()),
    scalaCompilerPlugins = Array.empty,
    testScalaCompilerPlugins = Array.empty,
    pluginDependencies = Array.empty
  )
  
  object SbtTestInterface extends JavaDependency.Maker:
    override def group: String = "org.scala-sbt"
    override def artifact: String = "test-interface"
    override def versionDefault: Version = Version("1.0")
    override def description: String = "SBT testing interface; some test frameworks (ScalaTest :)) do not bring it in in."
