package org.podval.tools.build

import org.gradle.api.Project
import org.podval.tools.gradle.{Artifact, Projects}
import org.podval.tools.platform.{Files, Output}
import java.io.File

trait DependencyInstallable[T] extends Dependency.WithScalaVersion:
  def repository: Option[Artifact.Repository] = None

  // Where retrieved distributions are cached
  def cacheDirectory: String

  def archiveSubdirectoryPath(version: Version): Seq[String]

  def isZip(version: Version): Boolean

  def installation(root: File): T

  def exists(installation: T): Boolean

  def fixup(installation: T): Unit

  def fromOs: Option[T]

  final def getInstalled(
    version: Option[Version],
    gradleUserHomeDir: File,
    output: Output
  ): T = get(
    version = version,
    gradleUserHomeDir = gradleUserHomeDir,
    output = output,
    ifDoesNotExist = (dependencyWithVersion, _) => output.abort(s"Needed dependency does not exist: $dependencyWithVersion")
  )

  final def getInstalledOrInstall(
    version: Option[Version],
    project: Project,
    output: Output
  ): T =
    val gradleUserHomeDir: File = Projects.gradleUserHomeDir(project)
    get(
      version = version,
      gradleUserHomeDir = gradleUserHomeDir,
      output = output,
      ifDoesNotExist = (dependencyWithVersion, result) => install(
        dependencyWithVersion, 
        result,
        gradleUserHomeDir,
        project,
        output
      )
    )

  private def get(
    version: Option[Version],
    gradleUserHomeDir: File,
    output: Output,
    ifDoesNotExist: (Dependency.WithVersion, T) => Unit
  ): T =
    def getInternal(version: Version) =
      val withVersion: Dependency.WithVersion = this.withVersion(version)
      val result: T = installation(root = Files.fileSeq(
        installsInto(gradleUserHomeDir, withVersion),
        archiveSubdirectoryPath(version)
      ))
  
      if exists(result)
      then output.info("DependencyInstallable", s"Existing $withVersion detected: $result")
      else ifDoesNotExist(withVersion, result)
      result
    
    version
      .map(getInternal)
      .orElse(fromOs)
      .getOrElse:
        val version: Version = dependency.versionDefault
        output.info("DependencyInstallable", s"Needed dependency is not installed locally and no version to install is specified: $this; installing default version: $version")
        getInternal(version)

  private def install(
    withVersion: Dependency.WithVersion,
    result: T,
    gradleUserHomeDir: File,
    project: Project,
    output: Output
  ): Unit =
    output.lifecycle("DependencyInstallable", s"Installing $withVersion as $result")

    val artifact: File = Artifact
      .resolve(
        project,
        withVersion.dependencyNotation(backendOverride = None),
        repository
      )
      .getOrElse(output.abort(s"No artifact found for: $withVersion"))

    val installsInto: File = this.installsInto(gradleUserHomeDir, withVersion)
    output.info("DependencyInstallable", s"Unpacking $artifact into $installsInto")

    Artifact.unpack(
      project,
      artifact,
      installsInto
    )

    if !exists(result) then output.abort(s"Does not exist after installation: $result")
    fixup(result)

  // Although Gradle caches resolved artifacts and npm caches packages that it retrieves,
  // unpacking frameworks under `/build` after each `./gradlew clean` takes noticeable time (around 14 seconds);
  // so, I am caching unpacked frameworks under `~/.gradle`.
  private def installsInto(
    gradleUserHomeDir: File,
    withVersion: Dependency.WithVersion
  ): File = Files.file(
    gradleUserHomeDir,
    cacheDirectory,
    withVersion.fileName
  )
