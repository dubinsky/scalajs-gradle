package org.podval.tools.build

import org.gradle.api.Project
import org.gradle.api.artifacts.{Configuration, Dependency}
import org.gradle.api.artifacts.repositories.{ArtifactRepository, IvyArtifactRepository, IvyPatternRepositoryLayout}
import org.gradle.api.file.CopySpec
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.SourceSet
import org.gradle.process.{ExecOperations, JavaExecSpec}
import java.io.File
import Gradle.getSourceSet

final class GradleBuildContext(project: Project, execOperations: ExecOperations)
  extends GradleBuildContextCore(
    gradleUserHomeDir = project.getGradle.getGradleUserHomeDir,
    logger = project.getLogger
  ) with BuildContext[Logger]:
  
  override def getArtifact(
    repository: Option[Repository],
    dependencyNotation: String
  ): Option[File] =
    var allRepositories: java.util.List[ArtifactRepository] = null

    if repository.isDefined then
      // Stash all the repositories
      allRepositories = java.util.ArrayList[ArtifactRepository]()
      allRepositories.addAll(project.getRepositories)
      project.getRepositories.clear()

      // Add repository
      project.getRepositories.ivy((newRepository: IvyArtifactRepository) =>
        newRepository.setUrl(repository.get.url)
        newRepository.patternLayout((repositoryLayout: IvyPatternRepositoryLayout) =>
          repositoryLayout.artifact(repository.get.artifactPattern)
          repositoryLayout.ivy(repository.get.ivy)
        )

        // Gradle 6.0 broke Node.js retrieval;
        // from https://github.com/gradle/gradle/issues/11006 and code referenced there
        // https://github.com/gradle/gradle/blob/b189979845c591d8c4a0032527383df0f6d679b2/subprojects/javascript/src/main/java/org/gradle/plugins/javascript/base/JavaScriptRepositoriesExtension.java#L53
        // it seems that to re-gain Gradle 5.6 behaviour, this needs to be done:
        newRepository.metadataSources((metadataSources: IvyArtifactRepository.MetadataSources) =>
          metadataSources.artifact(); // Indicates that this repository may not contain metadata files...
        )
      )

    info(s"Resolving $dependencyNotation")

    val dependency: Dependency = project.getDependencies.create(dependencyNotation)
    val configuration: Configuration = project.getConfigurations.detachedConfiguration(dependency)
    configuration.setDescription(s"Detached Configuration for resolving $dependencyNotation")
    configuration.setTransitive(false)

    try
      val result: File = configuration.getSingleFile
      info(s"Resolved: $result")
      Some(result)
    catch
      case _: IllegalStateException =>
        logger.warn(s"Failed to resolve: $dependencyNotation")
        None
    finally
      if allRepositories != null then
        // Restore original repositories
        project.getRepositories.clear()
        project.getRepositories.addAll(allRepositories)

  override def unpackArchive(file: File, isZip: Boolean, into: File): Unit =
    info(s"Unpacking $file into $into")

    into.mkdir()
    project.copy((copySpec: CopySpec) =>
      copySpec
        .from(if isZip then project.zipTree(file) else project.tarTree(file))
        .into(into)
      ()
    )

  override def javaexec(mainClass: String, args: String*): Unit =
    info(s"Running $mainClass(${args.mkString(", ")})")

    execOperations.javaexec((exec: JavaExecSpec) =>
      exec.setClasspath(project.getSourceSet(SourceSet.MAIN_SOURCE_SET_NAME).getRuntimeClasspath)
      exec.getMainClass.set(mainClass)
      exec.args(args*)
      ()
    )
