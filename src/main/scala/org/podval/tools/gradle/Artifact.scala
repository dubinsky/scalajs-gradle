package org.podval.tools.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.repositories.{ArtifactRepository, IvyArtifactRepository, IvyPatternRepositoryLayout}
import org.gradle.api.file.FileTree
import scala.jdk.CollectionConverters.IterableHasAsScala
import java.io.File

object Artifact:
  final class Repository(
    val url: String,
    val artifactPattern: String,
    val ivy: String
  )

  def resolve(
    project: Project,
    dependencyNotation: String,
    repository: Option[Repository]
  ): Option[File] = resolve(
    project,
    dependencyNotation,
    repository,
    transitive = false,
    resolve = (configuration: Configuration) =>
      try
        val result: File = configuration.getSingleFile
        project.getLogger.info(s"Resolved $dependencyNotation: $result", null, null, null)
        Some(result)
      catch
        case _: IllegalStateException =>
          project.getLogger.info(s"Failed to resolve $dependencyNotation", null, null, null)
          None
  )

  def resolveTransitive(
    project: Project,
    dependencyNotation: String,
    repository: Option[Repository]
  ): Iterable[File] = resolve(
    project,
    dependencyNotation,
    repository,
    transitive = true,
    resolve = (configuration: Configuration) =>
      val result: Iterable[File] = configuration.asScala
      project.getLogger.info(s"Resolved $dependencyNotation: $result", null, null, null)
      result
  )

  private def resolve[R](
    project: Project,
    dependencyNotation: String,
    repository: Option[Repository],
    transitive: Boolean,
    resolve: Configuration => R
  ): R =
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

    project.getLogger.info(s"Resolving $dependencyNotation", null, null, null)

    try
      resolve(Configurations.detached(project, dependencyNotation, transitive))
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
