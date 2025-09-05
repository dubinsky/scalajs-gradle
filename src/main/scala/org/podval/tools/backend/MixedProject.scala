package org.podval.tools.backend

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.podval.tools.build.ScalaBackend
import org.podval.tools.gradle.{Projects, Sources, Tasks}
import org.podval.tools.platform.Strings

object MixedProject:
  private val sharedSourceRoot: String = "shared"

  def apply(project: Project): Option[MixedProject] =
    val backends: Set[ScalaBackend] = Projects
      .subProjects(project)
      .flatMap(ScalaBackend.bySourceRoot)

    Option.when(backends.nonEmpty)(new MixedProject(project, backends))

final class MixedProject private(project: Project, backends: Set[ScalaBackend]) extends BackendProject(project):
  private val pojectsWithBackend: Set[ProjectWithBackend] = backends
    .map(backend => ProjectWithBackend(findSubproject(backend.sourceRoot), backend))

  private val sharedProjects: Set[SharedProject] = Projects
    .subProjects(project)
    .filter(sourceRoot => ScalaBackend.bySourceRoot(sourceRoot).isEmpty)
    .flatMap(sourceRoot => sharedBackends(sourceRoot).map(SharedProject(findSubproject(sourceRoot), _)))

  // Verify that there are no duplicate backends among the shared projects.
  sharedProjects
    .groupBy(_.backends)
    .filterNot(_._2.size == 1)
    .foreach:
      case (backends: Set[ScalaBackend], projects: Set[SharedProject]) =>
        error(s"""duplicate shared directories for backends
               |${ScalaBackend.sourceRoots(backends)}
               |${WithProject.names(projects)}
               |""".stripMargin)

  private def findSubproject(sourceRoot: String): Project = Projects
    .findSubproject(project, sourceRoot)
    .getOrElse(error(s"subproject '$sourceRoot' must be included in 'settings.gradle'"))

  private def sharedBackends(sourceRoot: String): Option[Set[ScalaBackend]] =
    import MixedProject.sharedSourceRoot
    Option.when(sourceRoot == sharedSourceRoot)(ScalaBackend.all).orElse:
      def error(message: String): Nothing = this.error(s"shared source root '$sourceRoot': $message")
      val backendNames: Array[String] = Strings.dropPrefixIfPresent(sourceRoot, s"$sharedSourceRoot-").split("-", -1)
      val backendsSeq: Seq[ScalaBackend] = backendNames.toSeq.flatMap(ScalaBackend.bySourceRoot)
      Option.when(backendsSeq.length == backendNames.length):
        val backends: Set[ScalaBackend] = backendsSeq.toSet
        if backends.size != backendNames.length then error(s"duplicate backend names")
        if backends.size == ScalaBackend.all.size then error(s"code shared between all the backends belongs in '$sharedSourceRoot'")
        if backends.size == 1 then error(s"code specific to the '${backends.head.name}' backend belongs in '${backends.head.sourceRoot}'")
        backends

  def findProject(
    candidate: Project,
    jvmPluginServices: JvmPluginServices
  ): Option[SingleProject] = sharedProjects
    .find(_.is(candidate))
    .orElse:
      pojectsWithBackend
        .find(_.is(candidate))
        .map(_.backend)
        .map: (backend: ScalaBackend) =>
          SingleBackendProject(
            candidate,
            jvmPluginServices,
            backend,
            sharedProjects =
              (sharedProjects ++ Option.when(Sources.exist(project))(SharedProject(project, ScalaBackend.all)).toSet)
                .filter(_.backends.contains(backend))
          )

  override def apply(): Unit =
    announce(s"using Scala backends ${ScalaBackend.names(pojectsWithBackend.map(_.backend))}")

    // Apply plugin to subprojects.
    (pojectsWithBackend ++ sharedProjects).foreach(_.project.getPluginManager.apply(classOf[BackendPlugin]))

    // Disable tasks.
    Tasks.disable(project, classOf[SourceTask])
    Tasks.disable(project, classOf[AbstractArchiveTask])
