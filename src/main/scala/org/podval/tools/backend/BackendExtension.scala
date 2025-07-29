package org.podval.tools.backend

import groovy.lang.Closure
import org.gradle.api.Project
import org.podval.tools.build.{ScalaBackend, ScalaDependencyMaker, ScalaLibrary, ScalaVersion, Version}
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

  final def getBackendVersion: String = nonJvm.backendVersion(project, getScalaLibrary).toString
  final def getNonJvmJUnit4present: Boolean = nonJvm.junit4present(project)
  
  final def isScala3: Boolean = getScalaLibrary.isScala3
  final def getScalaVersion: String = getScalaLibrary.scalaVersion.toString
  final def getScalaBinaryVersion: Version = getScalaLibrary.scalaVersion.binaryVersion.versionSuffix
  final def getScala2BinaryVersion: Version = getScalaLibrary.scala2.binaryVersion.versionSuffix

  final lazy val getScalaLibrary: ScalaLibrary =
    val result: ScalaLibrary = ScalaLibrary.fromImplementationConfiguration(project)

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
      getScalaLibrary,
      version
    )
    .dependencyNotation

  def getPluginScalaBinaryVersion: Version = getPluginScalaLibrary.scalaVersion.binaryVersion.versionSuffix
  def getPluginScala2BinaryVersion: Version = getPluginScalaLibrary.scala2.binaryVersion.versionSuffix

  final lazy val getPluginScalaLibrary: ScalaLibrary = ScalaLibrary.fromAmbientClasspath(project)

  import BackendExtension.Configure

  final def scalaDependency(group: String, artifact: String, version: String): String =
    scalaDependency(group, artifact, version, BackendExtension.idClosure)

  final def scalaDependency(group: String, artifact: String, version: String, transformer: Configure): String =
    dependency(group, artifact, version, transformer, getScalaLibrary)

  final def pluginDependency(group: String, artifact: String, version: String): String =
    pluginDependency(group, artifact, version, BackendExtension.idClosure)

  final def pluginDependency(group: String, artifact: String, version: String, transformer: Configure): String =
    dependency(group, artifact, version, transformer.andThen(BackendExtension.jvmClosure), getPluginScalaLibrary)

  private def dependency(
    groupId: String,
    artifactId: String,
    version: String,
    transformer: Configure,
    scalaLibrary: ScalaLibrary
  ): String =
    val scalaDependency: ScalaDependencyMaker = new ScalaDependencyMaker:
      override def group: String = groupId
      override def artifact: String = artifactId
      override def versionDefault: Version = Version(version)
      override def description: String = s"$group:$artifact:$versionDefault"
      override def scalaBackend: ScalaBackend = getBackend

    transformer
      .call(scalaDependency)
      .dependency(scalaLibrary)
      .withVersion(scalaLibrary, getBackend, None)
      .dependencyNotation

object BackendExtension:
  private type Configure = Closure[ScalaDependencyMaker]

  private def idClosure: Configure = Closure.IDENTITY.asInstanceOf[Configure]

  private def jvmClosure: Configure = new Configure(null):
    override def call(arguments: Any*): ScalaDependencyMaker = arguments
      .head
      .asInstanceOf[ScalaDependencyMaker]
      .jvm
