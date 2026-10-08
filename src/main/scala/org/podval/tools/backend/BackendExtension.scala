package org.podval.tools.backend

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.podval.tools.build.{Artifact, Backend, DependencyVersion, ScalaBinaryVersion, ScalaDependency, ScalaLibrary,
  TestFramework, Version}
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.nonjvm.NonJvmBackend
import org.podval.tools.scalajs.ScalaJSBackend
import org.podval.tools.scalanative.ScalaNativeBackend
import org.podval.tools.util.Extensions
import javax.inject.Inject

// Note: Gradle extensions must be abstract.
abstract class BackendExtension @Inject(
  project: Project,
  val getBackend: Backend,
  val isRunningInIntelliJ: Boolean
) extends WithProject(project):
  final def getName      : String = getBackend.name
  final def getSourceRoot: String = getBackend.sourceRoot
  final def getSuffix    : String = Artifact.suffix(getBackend, getScalaLibrary)

  def getUseArtifactSuffix: Property[Boolean]
  // Read from a follow-up afterEvaluate on this project, after this project's build script
  // and its afterEvaluate. The mixed parent was configured first.
  // An explicit value wins over the inherited convention.
  UseArtifactSuffix.configure(getUseArtifactSuffix)

  private def nonJvmBackend: NonJvmBackend = getBackend match
    case nonJvm: NonJvmBackend => nonJvm
    case backend => error(s"backend must be a non-JVM backend, not ${backend.name}")

  final def getBackendVersion: String = nonJvmBackend.backendVersion(project, getScalaLibrary).toString
  final def getNonJvmJUnit4present: Boolean = nonJvmBackend.junit4present(project)
  
  final def isScala3: Boolean = getScalaLibrary.scalaVersion.binaryVersion match
    case _: ScalaBinaryVersion.Scala3 => true
    case _ => false

  // Groovy exposes these as jvm, js, and native. Kotlin keeps the is prefix.
  final def isJvm: Boolean = getBackend eq JvmBackend
  final def isJs: Boolean = getBackend eq ScalaJSBackend
  final def isNative: Boolean = getBackend eq ScalaNativeBackend

  final def getScalaVersion: String = getScalaLibrary.scalaVersion.toString
  final def getScalaBinaryVersion: Version = getScalaLibrary.scalaBinaryVersionPrefix
  final def getScala2BinaryVersion: Version = getScalaLibrary.scala2BinaryVersionPrefix

  final lazy val getScalaLibrary: ScalaLibrary =
    val result: ScalaLibrary = ScalaLibrary.fromImplementationConfiguration(project)
    require(result.scalaVersion == getScalaVersionFromScalaExtension)
    result

  final def getPluginScalaBinaryVersion: Version = getPluginScalaLibrary.scalaBinaryVersionPrefix
  final def getPluginScala2BinaryVersion: Version = getPluginScalaLibrary.scala2BinaryVersionPrefix
  final lazy val getPluginScalaLibrary: ScalaLibrary = ScalaLibrary.fromAmbientClasspath(project)

  final def testFramework(frameworkClass: Class[? <: TestFramework]): String =
    testFramework(frameworkClass, None)

  final def testFramework(frameworkClass: Class[? <: TestFramework], version: String): String =
    testFramework(frameworkClass, Some(Version(version)))

  private def testFramework(
    frameworkClass: Class[? <: TestFramework],
    version: Option[Version]
  ): String = TestFramework
    .find(_.getClass.getName.startsWith(frameworkClass.getName), frameworkClass.toString)
    .withVersion(
      backend = getBackend,
      scalaLibrary = getScalaLibrary,
      version = version
    )
    .dependencyNotation

  final def scalaDependency(group: String, artifact: String, version: String): String =
    scalaDependency(group, artifact, version, BackendExtension.idConfigure)

  final def scalaDependency(group: String, artifact: String, version: String, configure: BackendExtension.Configure): String =
    withVersion(group, artifact, version, identity, configure, getScalaLibrary).dependencyNotation

  final def pluginDependency(group: String, artifact: String, version: String): String =
    pluginDependency(group, artifact, version, BackendExtension.idConfigure)

  final def pluginDependency(group: String, artifact: String, version: String, configure: BackendExtension.Configure): String =
    withVersion(group, artifact, version, _.jvm, configure, getPluginScalaLibrary).dependencyNotation

  private def withVersion(
    group: String,
    artifact: String,
    version: String,
    transform: ScalaDependency => ScalaDependency,
    configure: BackendExtension.Configure,
    scalaLibrary: ScalaLibrary
  ): DependencyVersion = configure.call(
    transform(
      ScalaDependency(
        backend = getBackend,
        name = s"$group:$artifact:$version",
        group = group,
        versionDefault = Version(version),
        artifact = artifact
      )
    )
  )
    .withVersion(
      scalaLibrary = scalaLibrary,
      version = Version(version)
    )

object BackendExtension:
  private type Configure = Closure[ScalaDependency]
  
  private def idConfigure: Configure = Closure.IDENTITY.asInstanceOf[Configure]

  val extensionName: String = "scalaBackend"

  def get(project: Project): BackendExtension = Extensions.getByName(project, extensionName)

  def create(
    project: Project,
    backend: Backend,
    isRunningInIntelliJ: Boolean
  ): BackendExtension = Extensions.create(
    project,
    extensionName,
    classOf[BackendExtension],
    backend,
    isRunningInIntelliJ
  )
