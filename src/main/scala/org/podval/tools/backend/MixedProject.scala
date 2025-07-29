package org.podval.tools.backend

import org.gradle.api.Project
import org.podval.tools.build.ScalaBackend
import org.podval.tools.gradle.Projects
import org.podval.tools.util.{Files, Strings}
import MixedProject.{BackendProject, SharedProject}

final class MixedProject private(
  backendProjects: Set[BackendProject],
  sharedProjects: Set[SharedProject]
):
  def backendNames: Set[String] = backendProjects.map(_.backend.name)

  def projects: Set[Project] = backendProjects.map(_.project) ++ sharedProjects.map(_.project)

  def sharedProject(project: Project): Option[SharedProject] = sharedProjects
    .find(sharedProject => Projects.isProjectDir(project, sharedProject.sourceRoot))

  def sharedProjectsForBackend(backend: ScalaBackend): Set[SharedProject] = sharedProjects
    .filter(_.backends.contains(backend))

object MixedProject:
  sealed abstract class SubProject(
    parent: Project,
    name: String
  ):
    val project: Project = Projects
    .findSubproject(parent, name)
    .getOrElse(BackendPlugin.error(parent, s"subproject '$name' must be included in 'settings.gradle'"))

  private final class BackendProject(
    parent: Project,
    val backend: ScalaBackend
  ) extends SubProject(
    parent,
    name = backend.sourceRoot
  )

  final class SharedProject(
    parent: Project,
    val sourceRoot: String,
    val backends: Set[ScalaBackend]
  ) extends SubProject(
    parent,
    name = sourceRoot
  )
  
  private val sharedSourceRoot: String = "shared"
  
  def apply(project: Project): Option[MixedProject] =
    val sourceRoots: Seq[String] = Files.listDirectories(Projects.projectDir(project)).map(_.getName)
    val trigger: Boolean = sourceRoots.flatMap(ScalaBackend.bySourceRoot).nonEmpty
    if !trigger then None else
      val subProjects: Set[SubProject] = sourceRoots.toSet.flatMap(sourceRoot =>
        ScalaBackend.bySourceRoot(sourceRoot) match
          case Some(backend: ScalaBackend) => Some(BackendProject(project, backend))
          case None => sharedBackends(project, sourceRoot).map(SharedProject(project, sourceRoot, _))
      )
      val backendProjects: Set[BackendProject] = subProjects.collect { case p: BackendProject => p }
      val sharedProjects : Set[SharedProject ] = subProjects.collect { case p: SharedProject  => p }

      sharedProjects.groupBy(_.backends).filterNot(_._2.size == 1).foreach:
        case (backends: Set[ScalaBackend], projects: Set[SharedProject]) =>
          val backendsString: String = backends.map(_.sourceRoot).mkString(", ")
          val projectsString: String = projects.map(_.sourceRoot).mkString(", ")
          BackendPlugin.error(project, s"duplicate shared directories for backends ($backendsString): $projectsString")

      Some(new MixedProject(
        backendProjects = backendProjects,
        sharedProjects  = sharedProjects
      ))

  private def sharedBackends(project: Project, sourceRoot: String): Option[Set[ScalaBackend]] =
    def error(message: String): Nothing = BackendPlugin.error(project, s"shared source root '$sourceRoot': $message")
    
    if sourceRoot == sharedSourceRoot then Some(ScalaBackend.all) else
      val suffix: String =
        if sourceRoot.startsWith(sharedSourceRoot)
        then Strings.drop(sourceRoot, s"$sharedSourceRoot-")
        else sourceRoot
      val backendNames: Array[String] = suffix.split("-", -1)
      val backendOpts: Set[Option[ScalaBackend]] = backendNames.toSet.map(ScalaBackend.bySourceRoot)
      if backendOpts.exists(_.isEmpty) then None else
        val backends: Set[ScalaBackend] = backendOpts.map(_.get)
        if backends.size != backendNames.length then error("duplicate backend names")
        if backends.size == ScalaBackend.all.size then error(s"code shared between all the backends belongs in '$sharedSourceRoot'")
        if backends.size == 1 then
          val backend: ScalaBackend = backends.head
          error(s"code specific to the '${backend.name}' backend belongs in '${backend.sourceRoot}'")
        Some(backends)
