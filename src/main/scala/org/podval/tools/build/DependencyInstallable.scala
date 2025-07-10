package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.repositories.{ArtifactRepository, IvyArtifactRepository, IvyPatternRepositoryLayout}
import org.gradle.api.file.FileTree
import org.gradle.api.{GradleException, Project}
import org.podval.tools.platform.TaskWithGradleUserHomeDir
import org.podval.tools.util.Files
import org.slf4j.{Logger, LoggerFactory}
import java.io.File

object DependencyInstallable:
  private val logger: Logger = LoggerFactory.getLogger(DependencyInstallable.getClass)

  def getGradleUserHomeDir(project: Project): File = project.getGradle.getGradleUserHomeDir

  final class Repository(
    val url: String,
    val artifactPattern: String,
    val ivy: String
  )

trait DependencyInstallable[T] extends Dependency:
  import DependencyInstallable.logger
  
  def repository: Option[DependencyInstallable.Repository] = None

  // Where retrieved distributions are cached
  def cacheDirectory: String = maker.artifact

  def archiveSubdirectoryPath(version: PreVersion): Seq[String] = Seq.empty

  def isZip(version: PreVersion): Boolean = false

  def installation(root: File): T

  def exists(installation: T): Boolean

  def fixup(installation: T): Unit = ()

  def fromOs: Option[T] = None

  private def fatalError(message: String): Nothing = throw GradleException(s"Fatal error in $this: $message")

  final def getInstalled(version: Option[String], gradleUserHomeDir: File): T = get(
    version = version,
    gradleUserHomeDir = gradleUserHomeDir,
    ifDoesNotExist = (dependencyWithVersion, _) => fatalError(s"Needed dependency does not exist: $dependencyWithVersion")
  )

  final def getInstalledOrInstall(version: Option[String], project: Project): T =
    val gradleUserHomeDir: File = DependencyInstallable.getGradleUserHomeDir(project)
    get(
      version = version,
      gradleUserHomeDir = gradleUserHomeDir,
      ifDoesNotExist = (dependencyWithVersion, result) => install(
        gradleUserHomeDir, 
        project,
        dependencyWithVersion, 
        result
      )
    )

  private def get(
    version: Option[String],
    gradleUserHomeDir: File,
    ifDoesNotExist: (Dependency#WithVersion, T) => Unit
  ): T =
    def getInternal(version: Version) =
      val dependencyWithVersion: Dependency#WithVersion = withVersion(version)
      val result: T = installation(root = Files.fileSeq(
        installsInto(gradleUserHomeDir, dependencyWithVersion),
        archiveSubdirectoryPath(dependencyWithVersion.version)
      ))
  
      if exists(result)
      then logger.info(s"Existing $dependencyWithVersion detected: $result")
      else ifDoesNotExist(dependencyWithVersion, result)
      result
    
    version match
      case Some(version) => getInternal(Version(version))
      case None => fromOs.getOrElse:
        logger.info(s"Needed dependency is not installed locally and no version to install is specified: $this; installing default version: ${maker.versionDefault}")
        getInternal(maker.versionDefault)

  private def installsInto(
    gradleUserHomeDir: File,
    dependencyWithVersion: Dependency#WithVersion
  ): File =
    // Although Gradle caches resolved artifacts and npm caches packages that it retrieves,
    // unpacking frameworks under `/build` after each `./gradlew clean` takes noticeable time (around 14 seconds);
    // so, I am caching unpacked frameworks under `~/.gradle`.
    Files.file(gradleUserHomeDir, cacheDirectory, dependencyWithVersion.fileName)

  private def install(
    gradleUserHomeDir: File,
    project: Project,
    dependencyWithVersion: Dependency#WithVersion,
    result: T
  ): Unit =
    logger.warn(s"Installing $dependencyWithVersion as $result")

    val artifact: File = getArtifact(
      project,
      dependencyWithVersion.dependencyNotation
    )
      .getOrElse(fatalError(s"No artifact found for: $dependencyWithVersion"))

    val from: FileTree = (if isZip(dependencyWithVersion.version) then project.zipTree else project.tarTree)(artifact)
    val into: File = installsInto(gradleUserHomeDir, dependencyWithVersion)

    logger.info(s"Unpacking $artifact into $into")
    into.mkdir()
    project.copy(_.from(from).into(into): Unit)

    if !exists(result) then fatalError(s"Does not exist after installation: $result")
    fixup(result)

  private def getArtifact(project: Project, dependencyNotation: String): Option[File] =
    var allRepositories: java.util.List[ArtifactRepository] = null

    if repository.isDefined then
      // Stash all the repositories
      allRepositories = java.util.ArrayList[ArtifactRepository]()
      allRepositories.addAll(project.getRepositories)
      project.getRepositories.clear()

      // Add repository
      project.getRepositories.ivy: (newRepository: IvyArtifactRepository) =>
        newRepository.setUrl(repository.get.url)
        newRepository.patternLayout: (repositoryLayout: IvyPatternRepositoryLayout) =>
          repositoryLayout.artifact(repository.get.artifactPattern)
          repositoryLayout.ivy(repository.get.ivy)

        // Gradle 6.0 broke Node.js retrieval;
        // from https://github.com/gradle/gradle/issues/11006 and code referenced there
        // https://github.com/gradle/gradle/blob/b189979845c591d8c4a0032527383df0f6d679b2/subprojects/javascript/src/main/java/org/gradle/plugins/javascript/base/JavaScriptRepositoriesExtension.java#L53
        // it seems that to re-gain Gradle 5.6 behaviour, this needs to be done:
        // Indicates that this repository may not contain metadata files...
        newRepository.metadataSources((metadataSources: IvyArtifactRepository.MetadataSources) => metadataSources.artifact())

    logger.info(s"Resolving $dependencyNotation")

    val configuration: Configuration = project.getConfigurations.detachedConfiguration(
      project.getDependencies.create(dependencyNotation)
    )
    configuration.setDescription(s"Detached Configuration for resolving $dependencyNotation")
    configuration.setTransitive(false)

    try
      val result: File = configuration.getSingleFile
      logger.info(s"Resolved: $result")
      Some(result)
    catch
      case _: IllegalStateException =>
        logger.warn(s"Failed to resolve: $dependencyNotation")
        None
    finally
      // Restore original repositories
      if allRepositories != null then
        project.getRepositories.clear()
        project.getRepositories.addAll(allRepositories)
