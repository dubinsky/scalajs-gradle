package org.podval.tools.backend

import groovy.lang.Closure
import org.gradle.api.Project
import org.podval.tools.build.{ScalaBackend, ScalaDependency, ScalaLibrary, Version}
import org.podval.tools.gradle.Extensions
import org.podval.tools.nonjvm.NonJvmBackend
import org.podval.tools.test.framework.Framework

import javax.inject.Inject

// Note: Gradle extensions must be abstract.
abstract class BackendExtension @Inject(
  project: Project,
  val getBackend: ScalaBackend,
  val isRunningInIntelliJ: Boolean
) extends WithProject(project):
  final def getName      : String = getBackend.name
  final def getSourceRoot: String = getBackend.sourceRoot
  final def getSuffix    : String = getBackend.artifactNameSuffix(getScalaBinaryVersion)
  
  private def nonJvm: NonJvmBackend = getBackend match
    case nonJvm: NonJvmBackend => nonJvm
    case backend => error(s"backend must be a non-JVM backend, not ${backend.name}")

  final def getBackendVersion: String = nonJvm.backendVersion(project, getScalaLibrary).toString
  final def getNonJvmJUnit4present: Boolean = nonJvm.junit4present(project)
  
  final def isScala3: Boolean = getScalaLibrary.isScala3
  final def getScalaVersion: String = getScalaLibrary.scalaVersion.toString
  final def getScalaBinaryVersion: Version = getScalaLibrary.scalaBinaryVersionSuffix
  final def getScala2BinaryVersion: Version = getScalaLibrary.scala2BinaryVersionSuffix

  final lazy val getScalaLibrary: ScalaLibrary =
    val result: ScalaLibrary = ScalaLibrary.fromImplementationConfiguration(project)
    require(result.scalaVersion == getScalaVersionFromScalaExtension)
    result

  final def getPluginScalaBinaryVersion: Version = getPluginScalaLibrary.scalaBinaryVersionSuffix
  final def getPluginScala2BinaryVersion: Version = getPluginScalaLibrary.scala2BinaryVersionSuffix
  final lazy val getPluginScalaLibrary: ScalaLibrary = ScalaLibrary.fromAmbientClasspath(project)

  final def testFramework(frameworkClass: Class[? <: Framework]): String =
    testFramework(frameworkClass, None)

  final def testFramework(frameworkClass: Class[? <: Framework], version: String): String =
    testFramework(frameworkClass, Some(Version(version)))

  private def testFramework(
    frameworkClass: Class[? <: Framework],
    version: Option[Version]
  ): String = Framework
    .find(_.getClass.getName.startsWith(frameworkClass.getName), frameworkClass.toString)
    .dependencyNotation(
      scalaLibrary = getScalaLibrary,
      backendOverride = Some(getBackend), 
      versionOverride = version
    )

  final def scalaDependency(group: String, artifact: String, version: String): String =
    scalaDependency(group, artifact, version, BackendExtension.idConfigure)

  final def scalaDependency(group: String, artifact: String, version: String, configure: BackendExtension.Configure): String =
    dependencyNotation(group, artifact, version, identity, configure, getScalaLibrary)

  final def pluginDependency(group: String, artifact: String, version: String): String =
    pluginDependency(group, artifact, version, BackendExtension.idConfigure)

  final def pluginDependency(group: String, artifact: String, version: String, configure: BackendExtension.Configure): String =
    dependencyNotation(group, artifact, version, _.jvm, configure, getPluginScalaLibrary)

  private def dependencyNotation(
    group: String,
    artifact: String,
    version: String,
    transform: ScalaDependency => ScalaDependency,
    configure: BackendExtension.Configure,
    scalaLibrary: ScalaLibrary
  ): String = configure.call(transform(ScalaDependency(
    backend = getBackend,
    groupId = group,
    artifactId = artifact,
    what = s"$group:$artifact:$version",
    version = Version(version)
  ))).dependencyNotation(scalaLibrary)

object BackendExtension:
  private type Configure = Closure[ScalaDependency]
  
  private def idConfigure: Configure = Closure.IDENTITY.asInstanceOf[Configure]

  private val name: String = "scalaBackend"

  def get(project: Project): BackendExtension = Extensions.getByName(project, name)

  def create(
    project: Project,
    backend: ScalaBackend,
    isRunningInIntelliJ: Boolean
  ): BackendExtension = Extensions.create(
    project,
    name,
    classOf[BackendExtension],
    backend,
    isRunningInIntelliJ
  )
