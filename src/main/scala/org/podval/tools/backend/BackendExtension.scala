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
  final def getScalaBinaryVersion: Version = getScalaLibrary.scalaVersion.binaryVersionSuffix
  final def getScala2BinaryVersion: Version = getScalaLibrary.scala2.binaryVersionSuffix

  final lazy val getScalaLibrary: ScalaLibrary =
    val result: ScalaLibrary = ScalaLibrary.fromImplementationConfiguration(project)
    require(result.scalaVersion == getScalaVersionFromScalaExtension)
    result

  def getPluginScalaBinaryVersion: Version = getPluginScalaLibrary.scalaVersion.binaryVersionSuffix
  def getPluginScala2BinaryVersion: Version = getPluginScalaLibrary.scala2.binaryVersionSuffix
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
      backendOverride = Some(getBackend), 
      versionOverride = version, 
      scalaLibrary = getScalaLibrary
    )

  final def scalaDependency(group: String, artifact: String, version: String): String =
    scalaDependency(group, artifact, version, BackendExtension.id)

  final def scalaDependency(group: String, artifact: String, version: String, configure: BackendExtension.Configure): String =
    dependency(group, artifact, version, BackendExtension.toFunction(configure), getScalaLibrary)

  final def pluginDependency(group: String, artifact: String, version: String): String =
    pluginDependency(group, artifact, version, BackendExtension.id)

  final def pluginDependency(group: String, artifact: String, version: String, configure: BackendExtension.Configure): String =
    dependency(group, artifact, version, BackendExtension.toFunction(configure).compose(_.jvm), getPluginScalaLibrary)

  private def dependency(
    groupId: String,
    artifactId: String,
    version: String,
    f: ScalaDependency => ScalaDependency,
    scalaLibrary: ScalaLibrary
  ): String =
    val scalaDependency: ScalaDependency = ScalaDependency(
      backend = getBackend,
      groupId = groupId,
      artifactId = artifactId,
      version = Version(version),
      what = s"$groupId:$artifactId:$version"
    )

    f(scalaDependency).dependencyNotation(
      backendOverride = None,
      versionOverride = None,
      scalaLibrary = scalaLibrary
    )

object BackendExtension:
  val name: String = "scalaBackend"

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

  def get(project: Project): BackendExtension = Extensions.getByName(project, name)

  private type Configure = Closure[ScalaDependency]
  private def id: Configure = Closure.IDENTITY.asInstanceOf[Configure]
  private def toFunction(configure: Configure): ScalaDependency => ScalaDependency = configure.call
