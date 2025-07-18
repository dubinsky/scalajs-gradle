package org.podval.tools.backend

import org.gradle.api.{GradleException, Project}
import org.podval.tools.build.{ScalaBackend, ScalaBinaryVersion, ScalaLibrary, ScalaVersion, Version}
import org.podval.tools.gradle.{Projects, ScalaExtension}
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.nonjvm.NonJvmBackend
import org.podval.tools.platform.IntelliJIdea
import org.podval.tools.scalajs.ScalaJSBackend
import org.podval.tools.scalanative.ScalaNativeBackend
import org.podval.tools.test.framework.FrameworkDescriptor
import javax.inject.Inject

abstract class BackendExtension @Inject(project: Project):
  final def error(message: String): Nothing =
    throw GradleException(s"${pluginMessage(message)}\nDocumentation: https://github.com/dubinsky/scalajs-gradle")

  final def lifecycle(message: String): Unit =
    project.getLogger.lifecycle(pluginMessage(message))
  
  private final def pluginMessage(message: String): String =
    s"Plugin 'org.podval.tools.scalajs' in $project: $message."
  
  private var backend: Option[ScalaBackend] = None
  final def setBackend(backend: ScalaBackend): Unit = this.backend = Some(backend)
  final def getBackend: ScalaBackend = backend.getOrElse(error(s"Scala backend not set"))

  final def getName      : String = getBackend.name
  final def getSourceRoot: String = getBackend.sourceRoot
  final def getSuffix    : String = getBackend.artifactSuffixString
  
  final def isJvm   : Boolean = getBackend == JvmBackend
  final def isJs    : Boolean = getBackend == ScalaJSBackend
  final def isNative: Boolean = getBackend == ScalaNativeBackend

  private def nonJvm: NonJvmBackend = getBackend match
    case nonJvm: NonJvmBackend => nonJvm
    case backend => error(s"backend must be a non-JVM backend, not ${backend.name}")

  final def getBackendVersion: Version = nonJvm.backendVersion(project, getScalaVersion)
  final def isNonJvmJUnit4present: Boolean = nonJvm.junit4present(project)
  
  final def getScalaLibrary: ScalaLibrary = scalaLibrary
  final def getScalaVersion: ScalaVersion = getScalaLibrary.scalaVersion
  final def isScala3: Boolean = getScalaVersion.isScala3
  final def getMajor: Int = getScalaVersion.binaryVersion.versionMajor
  final def getScalaBinaryVersion: Version = getScalaVersion.binaryVersion.versionSuffix
  final def getScala2BinaryVersion: Version =
    (if !isScala3 then getScalaVersion.binaryVersion else ScalaBinaryVersion.Scala2.P13).versionSuffix

  private lazy val scalaLibrary: ScalaLibrary =
    val result: ScalaLibrary = ScalaLibrary.getFromImplementationConfiguration(project)

    val scalaVersion: ScalaVersion = ScalaExtension
      .findScalaVersion(project)
      .getOrElse(error(
        s"""Scala version data is not supported when Scala version is inferred from the Scala library dependency;
           |set Scala version on the Scala plugin's extension instead: `scala.scalaVersion=...`""".stripMargin
      ))

    require(result.scalaVersion == scalaVersion)
    result

  final def isRunningInIntelliJ: Boolean = IntelliJIdea.runningIn

  final def isBuildPerScalaVersion: Boolean = Projects
    .findProperty(project, BackendPlugin.buildPerScalaVersionProperty)
    .contains("true")

  final def testFramework(
    frameworkClass: Class[? <: FrameworkDescriptor]
  ): String = testFramework(
    frameworkClass,
    None
  )

  final def testFramework(
    frameworkClass: Class[? <: FrameworkDescriptor],
    version: String
  ): String = testFramework(
    frameworkClass,
    Some(Version(version))
  )

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
