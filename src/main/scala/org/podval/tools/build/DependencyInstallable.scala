package org.podval.tools.build

import org.gradle.api.Project
import org.podval.tools.gradle.{Artifact, Projects}
import org.podval.tools.platform.Output
import org.podval.tools.util.Files
import java.io.File

trait DependencyInstallable[T] extends Dependency:
  def repository: Option[Artifact.Repository] = None

  // Where retrieved distributions are cached
  def cacheDirectory: String = maker.artifact

  def archiveSubdirectoryPath(version: PreVersion): Seq[String] = Seq.empty

  def isZip(version: PreVersion): Boolean = false

  def installation(root: File): T

  def exists(installation: T): Boolean

  def fixup(installation: T): Unit = ()

  def fromOs: Option[T] = None

  final def getInstalled(
    version: Option[String],
    gradleUserHomeDir: File,
    output: Output
  ): T = get(
    version = version,
    gradleUserHomeDir = gradleUserHomeDir,
    output = output,
    ifDoesNotExist = (dependencyWithVersion, _) => output.abort(s"Needed dependency does not exist: $dependencyWithVersion")
  )

  final def getInstalledOrInstall(
    version: Option[String],
    project: Project,
    output: Output
  ): T = get(
    version = version,
    gradleUserHomeDir = Projects.gradleUserHomeDir(project),
    output = output,
    ifDoesNotExist = (dependencyWithVersion, result) => install(
      project,
      dependencyWithVersion, 
      result,
      output
    )
  )

  private def get(
    version: Option[String],
    gradleUserHomeDir: File,
    output: Output,
    ifDoesNotExist: (Dependency#WithVersion, T) => Unit
  ): T =
    def getInternal(version: Version) =
      val dependencyWithVersion: Dependency#WithVersion = withVersion(version)
      val result: T = installation(root = Files.fileSeq(
        installsInto(gradleUserHomeDir, dependencyWithVersion),
        archiveSubdirectoryPath(dependencyWithVersion.version)
      ))
  
      if exists(result)
      then output.info("DependencyInstallable", s"Existing $dependencyWithVersion detected: $result")
      else ifDoesNotExist(dependencyWithVersion, result)
      result
    
    version match
      case Some(version) =>
        getInternal(Version(version))
      case None =>
        fromOs.getOrElse:
          val version: Version = maker.versionDefault
          output.info("DependencyInstallable", s"Needed dependency is not installed locally and no version to install is specified: $this; installing default version: $version")
          getInternal(version)
  
  private def install(
    project: Project,
    dependencyWithVersion: Dependency#WithVersion,
    result: T,
    output: Output
  ): Unit =
    output.lifecycle("DependencyInstallable", s"Installing $dependencyWithVersion as $result")

    val artifact: File = Artifact.resolve(
      project,
      dependencyWithVersion.dependencyNotation,
      repository
    )
      .getOrElse(output.abort(s"No artifact found for: $dependencyWithVersion"))

    val gradleUserHomeDir: File = Projects.gradleUserHomeDir(project)
    val into: File = installsInto(gradleUserHomeDir, dependencyWithVersion)
    output.info("DependencyInstallable", s"Unpacking $artifact into $into")

    Artifact.unpack(
      project,
      artifact,
      into
    )

    if !exists(result) then output.abort(s"Does not exist after installation: $result")
    fixup(result)

  // Although Gradle caches resolved artifacts and npm caches packages that it retrieves,
  // unpacking frameworks under `/build` after each `./gradlew clean` takes noticeable time (around 14 seconds);
  // so, I am caching unpacked frameworks under `~/.gradle`.
  private def installsInto(
    gradleUserHomeDir: File,
    dependencyWithVersion: Dependency#WithVersion
  ): File = Files.file(
    gradleUserHomeDir,
    cacheDirectory,
    dependencyWithVersion.fileName
  )
