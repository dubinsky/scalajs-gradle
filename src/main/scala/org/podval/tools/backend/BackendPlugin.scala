package org.podval.tools.backend

import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.{Plugin, Project}
import org.podval.tools.build.ScalaBackend
import org.podval.tools.gradle.Projects
import org.podval.tools.jvm.JvmBackend
import javax.inject.Inject

final class BackendPlugin @Inject(
  project: Project,
  jvmPluginServices: JvmPluginServices
) extends Logging(project) with Plugin[Project]:
  override def apply(project: Project): Unit =
    // Apply Scala plugin to this project.
    Projects.applyPlugin(project, classOf[ScalaPlugin])

    // Determine what type of BackendProject we have and configure it.
    BackendPlugin
      .mixed(project)
      .orElse:
        Projects
          .parent(project)
          .flatMap(BackendPlugin.mixed)
          .flatMap: (parent: MixedProject) =>
            findProject(parent.sharedProjects, _.project)
              .orElse:
                findProject(parent.backendProjects, _.project)
                  .map(_.backend)
                  .map: (backend: ScalaBackend) =>
                    single(backend, sharedProjects = parent
                      .sharedProjects
                      .filter(_.backends.contains(backend))
                    )
      .getOrElse:
        single(sharedProjects = Set.empty, backend = Projects
          .findProperty(project, BackendPlugin.scalaBackendProperty)
          .map: (backendName: String) =>
            BackendPlugin
              .findBackend(backendName)
              .getOrElse:
                error(s"""unknown Scala backend '$backendName'; use one of
                         |${BackendPlugin.backendNames}""".stripMargin)
          .getOrElse:
            info(s"""to choose Scala backend, set property '${BackendPlugin.scalaBackendProperty}' to one of
                    |${BackendPlugin.backendNames};
                    |to use multiple backends, create at least one of the subprojects
                    |${ScalaBackend.all.map(_.sourceRoot).mkString(", ")}""".stripMargin)
            JvmBackend
        )
      .apply()

  private def findProject[T](candidates: Set[T], i: T => Project): Option[T] = candidates
    .find(candidate => Projects.projectDir(i(candidate)).getName == Projects.projectDir(project).getName)

  private def single(backend: ScalaBackend, sharedProjects: Set[SharedProject]) =
    SingleBackendProject(project, backend, jvmPluginServices, sharedProjects)

object BackendPlugin:
  val scalaBackendProperty: String = "org.podval.tools.backend"

  private def mixed(project: Project): Option[MixedProject] = Option
    .when(Projects
      .subProjects(project)
      .exists(ScalaBackend.bySourceRoot(_).isDefined)
    )(MixedProject(project))

  private def backendNames: String = ScalaBackend.all.map(backend => s"${backend.name} (${backend.sourceRoot})").mkString(", ")

  private def findBackend(name: String): Option[ScalaBackend] = ScalaBackend.all.find((backend: ScalaBackend) =>
    name.toLowerCase == backend.name      .toLowerCase ||
    name.toLowerCase == backend.sourceRoot.toLowerCase
  )
