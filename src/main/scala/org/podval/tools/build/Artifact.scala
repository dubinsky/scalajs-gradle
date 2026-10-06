package org.podval.tools.build

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency as GDependency
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.podval.tools.util.Extensions
import org.podval.tools.util.Strings.{prefix, split}
import scala.collection.mutable.ListBuffer
import java.io.File

final class Artifact(
  val group: Option[String],
  val name: String,
  val backend: Option[String],
  val scalaVersion: Option[String],
  val version: Option[String],
  val classifier: Option[String],
  val extension: Option[String]
):
  override def toString: String = dependencyNotation

  def fileName: String =
    nameToString +
    "-" +
    version.get +
    prefix("-", classifier) +
    "." +
    extension.getOrElse("jar")

  def dependencyNotation: String =
    group.get +
    ":" +
    nameToString +
    ":" +
    version.get +
    prefix(":", classifier) +
    prefix("@", extension)

  private def nameToString: String =
    name +
    prefix("_", backend) +
    prefix("_", scalaVersion)

object Artifact:
  def suffix(
    backend: Backend,
    scalaLibrary: ScalaLibrary
  ): String =
    // TODO name a release candidate with the full Scala version (`_3.8.0-RC1`), as sbt does.
    s"${prefix("_", backend.artifactSuffix)}_${scalaLibrary.scalaBinaryVersionPrefix}"

  def configureMavenPublications(
    project: Project,
    projectName: String,
    artifactSuffix: String
  ): Unit =
    if artifactSuffix.isEmpty then
      project.getLogger.info("Artifact suffix is disabled.", null, null, null)
    else project.getPluginManager.withPlugin(
      "maven-publish",
      _ =>
        val publishing: PublishingExtension =
          Extensions.getByType(project, classOf[PublishingExtension])
        val suffixedPublicationNames: ListBuffer[String] = ListBuffer.empty

        publishing.getPublications.withType(classOf[MavenPublication]).configureEach: (publication: MavenPublication) =>
          val current: String = publication.getArtifactId
          if current == projectName && !current.endsWith(".gradle.plugin") then
            publication.setArtifactId(projectName + artifactSuffix)
            val publicationName: String = publication.getName
            // The marker id is assigned after create, so a suffix here is temporary.
            if !publicationName.endsWith("PluginMarkerMaven") then
              suffixedPublicationNames += publicationName

        // Later afterEvaluate callbacks can still overwrite the id.
        project.getGradle.projectsEvaluated(_ =>
          val suffixedArtifactId: String = projectName + artifactSuffix
          suffixedPublicationNames.foreach: (publicationName: String) =>
            val publication: MavenPublication = publishing
              .getPublications
              .withType(classOf[MavenPublication])
              .getByName(publicationName)
            if publication.getArtifactId == suffixedArtifactId then
              project.getLogger.info(
                s"Publication '$publicationName' uses artifact suffix '$artifactSuffix'.",
                null, null, null
              )
        )
    )

  def fromFile(file: File): Artifact =
    val (nameAndVersion: String, extension: Option[String]) = split(file.getName, '.')
    val (name: String, version: Option[String]) = split(nameAndVersion, '-')
    from(
      group = None,
      nameString = name,
      version = version,
      classifier = None,
      extension = extension
    )

  def fromDependency(dependency: GDependency): Artifact =
    from(
      group = Option(dependency.getGroup),
      nameString = dependency.getName,
      version = Option(dependency.getVersion),
      classifier = None,
      extension = None
    )

  private def from(
    group: Option[String],
    nameString: String,
    version: Option[String],
    classifier: Option[String],
    extension: Option[String]
  ): Artifact =
    val (nameAndBackend: String, scalaVersion: Option[String]) = split(nameString, '_')
    val (name: String, backend: Option[String]) = split(nameAndBackend, '_')
    Artifact(
      group = group,
      name = name,
      backend = backend,
      scalaVersion = scalaVersion,
      version = version,
      classifier = classifier,
      extension = extension
    )
