package org.podval.tools.backendplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.scala.ScalaPluginExtension
import org.gradle.api.{GradleException, Project}
import org.gradle.api.provider.Property
import org.podval.tools.backend.ScalaBackend
import org.podval.tools.backend.jvm.JvmBackend
import org.podval.tools.backend.nonjvm.NonJvmBackend
import org.podval.tools.backend.scalajs.ScalaJSBackend
import org.podval.tools.backend.scalanative.ScalaNativeBackend
import org.podval.tools.build.{ScalaBinaryVersion, ScalaLibrary, ScalaVersion, Version}
import org.podval.tools.platform.IntelliJIdea
import javax.inject.Inject

abstract class BackendExtension @Inject(project: Project):
  final def error(message: String): Nothing =
    throw GradleException(s"${pluginMessage(message)}\nDocumentation: https://github.com/dubinsky/scalajs-gradle")

  final def lifecycle(message: String): Unit =
    project.getLogger.lifecycle(pluginMessage(message))
  
  private final def pluginMessage(message: String): String =
    s"Plugin 'org.podval.tools.scalajs' in $project: $message."

  final def isRunningInIntelliJ: Boolean = IntelliJIdea.runningIn

  private def getImplementationConfiguration(isTest: Boolean): Configuration =
    project.getConfigurations.getByName(Sources.getSourceSet(project, isTest).getImplementationConfigurationName)

  final def isBuildPerScalaVersion: Boolean =
    Option(project.findProperty(BackendPlugin.buildPerScalaVersionProperty)).contains("true")
  
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

  final def getBackendVersion: Version.Simple = nonJvm.backendVersion(
    getScalaVersion,
    getImplementationConfiguration(isTest = false)
  )

  final def isNonJvmJUnit4present: Boolean = nonJvm.junit4present(
    getImplementationConfiguration(isTest = true)
  )

  final def getScalaLibrary: ScalaLibrary =
    ScalaLibrary.getFromConfiguration(getImplementationConfiguration(isTest = false))

  final def getScalaExtensionScalaVersionProperty: Property[String] = project
    .getExtensions
    .getByType(classOf[ScalaPluginExtension])
    .getScalaVersion

  final def getScalaVersion: ScalaVersion = Option(getScalaExtensionScalaVersionProperty.getOrNull)
    .map(ScalaVersion(_))
    .getOrElse(error(
      s"""Scala version data is not supported when Scala version is inferred from the Scala library dependency;
         |set Scala version on the Scala plugin's extension instead: `scala.scalaVersion=...`""".stripMargin
    ))
  
  final def isScala3: Boolean = getScalaVersion.isScala3
  final def getMajor: Int = getScalaVersion.binaryVersion.versionMajor
  final def getScalaBinaryVersion: Version.Simple = getScalaVersion.binaryVersion.versionSuffix
  final def getScala2BinaryVersion: Version.Simple =
    (if !isScala3 then getScalaVersion.binaryVersion else ScalaBinaryVersion.Scala213).versionSuffix
