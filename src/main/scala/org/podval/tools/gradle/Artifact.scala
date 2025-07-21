package org.podval.tools.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.repositories.{ArtifactRepository, IvyArtifactRepository, IvyPatternRepositoryLayout}
import org.gradle.api.file.FileTree
import org.slf4j.{Logger, LoggerFactory}
import java.io.File

object Artifact:
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  final class Repository(
    val url: String,
    val artifactPattern: String,
    val ivy: String
  )

  def resolve(
    project: Project,
    dependencyNotation: String,
    repository: Option[Repository]
  ): Option[File] =
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

    val configuration: Configuration = Configurations.detached(project, dependencyNotation)
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

  def unpack(
    project: Project,
    artifact: File,
    into: File
  ): Unit =
    val isZip: Boolean = artifact.getName.endsWith(".zip")
    val from: FileTree = (if isZip then project.zipTree else project.tarTree)(artifact)
    into.mkdir()
    project.copy(_.from(from).into(into): Unit)
