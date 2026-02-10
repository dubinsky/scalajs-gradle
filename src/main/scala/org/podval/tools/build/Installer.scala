package org.podval.tools.build

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.podval.tools.util.{Files, Projects}
import java.io.File

trait Installer[T] extends Dependency:
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
    ifDoesNotExist: (DependencyVersion, T) => Unit
  ): T =
    def getInternal(version: Version) =
      val dependencyVersion: DependencyVersion = this.withVersion(
        version = version,
        scalaVersion = None
     )

      val result: T = installation(root = Files.fileSeq(
        installsInto(gradleUserHomeDir, dependencyVersion),
        archiveSubdirectoryPath(version)
      ))
  
      if exists(result)
      then output.info("Installer", s"Existing $dependencyVersion detected: $result")
      else ifDoesNotExist(dependencyVersion, result)
      result
    
    version
      .map(getInternal)
      .orElse(fromOs)
      .getOrElse:
        val version: Version = versionDefault
        output.info("Installer", s"Needed dependency is not installed locally and no version to install is specified: $this; installing default version: $version")
        getInternal(version)

  private def install(
    dependencyVersion: DependencyVersion,
    result: T,
    gradleUserHomeDir: File,
    project: Project,
    output: Output
  ): Unit =
    output.lifecycle("Installer", s"Installing $dependencyVersion as $result")

    val artifact: File = dependencyVersion.resolve(
      project = project,
      transitive = false,
      resolve = (configuration: Configuration) =>
        try
          configuration.getSingleFile
        catch
          case _: IllegalStateException =>
            output.abort(s"No artifact found for $dependencyVersion")
    )
    output.info("Installer", s"Resolved $dependencyVersion: $artifact")

    val installsInto: File = this.installsInto(gradleUserHomeDir, dependencyVersion)
    output.info("Installer", s"Unpacking $artifact into $installsInto")

    Files.unpack(
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
    dependencyVersion: DependencyVersion
  ): File = Files.file(
    gradleUserHomeDir,
    cacheDirectory,
    dependencyVersion.fileName
  )
