package org.podval.tools.scalajsplugin.jvm

import org.gradle.api.artifacts.Configuration
import org.podval.tools.build.{CreateExtension, JavaDependency, ScalaBackendKind, ScalaPlatform, Version}
import org.podval.tools.scalajsplugin.{BackendDelegate, BackendDependencyRequirements, BackendTask}

object JvmDelegate extends BackendDelegate[JvmTask]:
  override def taskClass: Class[JvmTask] = classOf[JvmTask]

  override def linkTaskClassOpt    : Option[Class[? <: JvmTask & BackendTask.Link.Main]] = None
  override def testLinkTaskClassOpt: Option[Class[? <: JvmTask & BackendTask.Link.Test]] = None

  override def runTaskClass : Class[JvmRunTask ] = classOf[JvmRunTask ]
  override def testTaskClass: Class[JvmTestTask] = classOf[JvmTestTask]

  override def backendKind: ScalaBackendKind = ScalaBackendKind.JVM
  override def pluginDependenciesConfigurationNameOpt: Option[String] = None
  override def createExtension: Option[CreateExtension[?]] = None
  override def scalaCompileParameters(isScala3: Boolean): Seq[String] = Seq.empty

  override def dependencyRequirements(
    implementationConfiguration: Configuration,
    testImplementationConfiguration: Configuration,
    projectScalaPlatform: ScalaPlatform
  ): BackendDependencyRequirements = BackendDependencyRequirements(
    implementation = Seq.empty,
    testImplementation = Seq(SbtTestInterface.required()),
    scalaCompilerPlugins = Seq.empty,
    pluginDependencies = Seq.empty
  )

  object SbtTestInterface extends JavaDependency.Maker:
    override def group: String = "org.scala-sbt"
    override def artifact: String = "test-interface"
    override def versionDefault: Version = Version("1.0")
    override def description: String = "SBT testing interface; some test frameworks (ScalaTest :)) do not bring it in in."
