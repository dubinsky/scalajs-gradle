package org.podval.tools.backend

import org.gradle.api.Project
import org.podval.tools.build.{ScalaBackend, ScalaBinaryVersion, ScalaDependencyMaker, ScalaLibrary, ScalaVersion, Version}
import org.podval.tools.gradle.ScalaExtension
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.nonjvm.NonJvmBackend
import org.podval.tools.scalajs.ScalaJSBackend
import org.podval.tools.scalanative.ScalaNativeBackend
import org.podval.tools.test.framework.FrameworkDescriptor
import javax.inject.Inject

abstract class BackendExtension @Inject(
  project: Project,
  val getBackend: ScalaBackend,
  val isRunningInIntelliJ: Boolean
):
  final def getName      : String = getBackend.name
  final def getSourceRoot: String = getBackend.sourceRoot
  final def getSuffix    : String = getBackend.artifactSuffixString
  
  final def isJvm   : Boolean = getBackend == JvmBackend
  final def isJs    : Boolean = getBackend == ScalaJSBackend
  final def isNative: Boolean = getBackend == ScalaNativeBackend

  private def nonJvm: NonJvmBackend = getBackend match
    case nonJvm: NonJvmBackend => nonJvm
    case backend => BackendPlugin.error(project, s"backend must be a non-JVM backend, not ${backend.name}")

  final def getBackendVersion: String = nonJvm.backendVersion(project, getScalaVersion).toString
  final def getNonJvmJUnit4present: Boolean = nonJvm.junit4present(project)
  
  final def isScala3: Boolean = getScalaVersion.isScala3
  final def getScalaBinaryVersion: Version = getScalaVersion.binaryVersion.versionSuffix
  final def getScala2BinaryVersion: Version = getScalaVersion.binary2Version.versionSuffix

  def getScalaVersion: ScalaVersion = getScalaLibrary.scalaVersion

  final lazy val getScalaLibrary: ScalaLibrary =
    val result: ScalaLibrary = ScalaLibrary.getFromImplementationConfiguration(project)

    val scalaVersion: ScalaVersion = ScalaExtension
      .findScalaVersion(project)
      .getOrElse(BackendPlugin.error(project,
        s"""Scala version data is not supported when Scala version is inferred from the Scala library dependency;
           |set Scala version on the Scala plugin's extension instead: `scala.scalaVersion=...`""".stripMargin
      ))

    require(result.scalaVersion == scalaVersion)
    result

  final def testFramework(frameworkClass: Class[? <: FrameworkDescriptor]): String =
    testFramework(frameworkClass, None)

  final def testFramework(frameworkClass: Class[? <: FrameworkDescriptor], version: String): String =
    testFramework(frameworkClass, Some(Version(version)))

  private def testFramework(
    frameworkClass: Class[? <: FrameworkDescriptor],
    version: Option[Version]
  ): String = FrameworkDescriptor
    .forClass(frameworkClass)
    .dependencyWithVersion(
      getBackend,
      getScalaVersion,
      version
    )
    .dependencyNotation

  def getPluginScalaVersion: ScalaVersion = getPluginScalaLibrary.scalaVersion
  def getPluginScalaBinaryVersion: ScalaBinaryVersion = getPluginScalaVersion.binaryVersion
  def getPluginScala2Version: ScalaVersion = getPluginScalaLibrary.scala2.get
  def getPluginScala2BinaryVersion: ScalaBinaryVersion = getPluginScala2Version.binaryVersion

  final lazy val getPluginScalaLibrary: ScalaLibrary = ScalaLibrary.getFromClasspath

  private class ScalaDependencyStub(
    final override val group: String,
    final override val artifact: String,
    version: String
  ) extends ScalaDependencyMaker:
    final override def versionDefault: Version = Version(version)
    final override def description: String = s"BackendExtension: $group:$artifact:$version"
    override def scalaBackend: ScalaBackend = getBackend

  private def dependencyNotation(scalaVersion: ScalaVersion)(maker: ScalaDependencyMaker): String = maker
    .dependency(scalaVersion)
    .withDefaultVersion
    .dependencyNotation

  private def projectDependencyNotation(maker: ScalaDependencyMaker): String =
    dependencyNotation(getScalaVersion)(maker)

  final def scalaDependency(group: String, artifact: String, version: String): String =
    projectDependencyNotation(new ScalaDependencyStub(group, artifact, version))

  final def scala3Dependency(group: String, artifact: String, version: String): String =
    projectDependencyNotation(new ScalaDependencyStub(group, artifact, version) with ScalaDependencyMaker.Scala3)

  final def scala2Dependency(group: String, artifact: String, version: String): String =
    projectDependencyNotation(new ScalaDependencyStub(group, artifact, version) with ScalaDependencyMaker.Scala2)

  final def scalaJvmDependency(group: String, artifact: String, version: String): String =
    projectDependencyNotation(new ScalaDependencyStub(group, artifact, version) with ScalaDependencyMaker.Jvm)

  final def scala2JvmDependency(group: String, artifact: String, version: String): String =
    projectDependencyNotation(new ScalaDependencyStub(group, artifact, version) with ScalaDependencyMaker.JvmScala2)

  final def scalaCompilerPlugin(group: String, artifact: String, version: String): String =
    projectDependencyNotation(new ScalaDependencyStub(group, artifact, version) with ScalaDependencyMaker.Jvm with ScalaDependencyMaker.IsScalaVersionFull)

  final def scala2CompilerPlugin(group: String, artifact: String, version: String): String =
    projectDependencyNotation(new ScalaDependencyStub(group, artifact, version) with ScalaDependencyMaker.JvmScala2 with ScalaDependencyMaker.IsScalaVersionFull)

  final def pluginScalaDependency(group: String, artifact: String, version: String): String =
    dependencyNotation(getPluginScalaVersion)(new ScalaDependencyStub(group, artifact, version) with ScalaDependencyMaker.Jvm)

  final def pluginScala2Dependency(group: String, artifact: String, version: String): String =
    dependencyNotation(getPluginScala2Version)(new ScalaDependencyStub(group, artifact, version) with ScalaDependencyMaker.Jvm)
