package org.podval.tools.build

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.repositories.{ArtifactRepository, IvyArtifactRepository, IvyPatternRepositoryLayout}

final class DependencyVersion(
  val dependency: Dependency,
  val version: Version.Pre,
  val scalaVersion: Option[Version]
):
  override def toString: String = dependencyNotation

  def dependencyNotation: String = artifact.dependencyNotation

  def fileName: String = artifact.fileName

  private def artifact: Artifact = Artifact(
    group = Some(dependency.group),
    name = dependency.artifact,
    backend = dependency.backendSuffix,
    scalaVersion = scalaVersion.map(_.toString),
    version = Some(version.toString),
    classifier = dependency.classifier(version.version),
    extension = dependency.extension(version.version)
  )

  def resolve[R](
    project: Project,
    transitive: Boolean,
    resolve: Configuration => R
  ): R =
    var allRepositories: java.util.List[ArtifactRepository] = null

    val repository: Option[Dependency.Repository] = dependency.repository
    
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

    project.getLogger.info(s"Resolving $this", null, null, null)

    try
      val configuration: Configuration = project.getConfigurations.detachedConfiguration(
        project.getDependencies.create(dependencyNotation)
      )
      configuration.setDescription(s"Detached Configuration for resolving $this")
      configuration.setTransitive(transitive)

      resolve(configuration)
    finally
      // Restore original repositories
      if allRepositories != null then
        project.getRepositories.clear()
        project.getRepositories.addAll(allRepositories)

