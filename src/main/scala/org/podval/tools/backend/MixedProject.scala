package org.podval.tools.backend

import org.gradle.api.Project
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.podval.tools.build.ScalaBackend
import org.podval.tools.gradle.{Configurations, Projects, Sources, Tasks}
import org.podval.tools.platform.Strings

object MixedProject:
  private val sharedSourceRoot: String = "shared"

final class MixedProject(project: Project) extends BackendProject(project):
  // Gather subprojects.
  private def findSubproject(sourceRoot: String): Project = Projects
    .findSubproject(project, sourceRoot)
    .getOrElse(error(s"subproject '$sourceRoot' must be included in 'settings.gradle'"))

  val backendProjects: Set[ProjectWithBackend] = Projects
    .subProjects(project)
    .flatMap(ScalaBackend.bySourceRoot)
    .map(backend => ProjectWithBackend(findSubproject(backend.sourceRoot), backend))

  require(backendProjects.nonEmpty)
  
  val sharedProjects: Set[SharedProject] = Projects
    .subProjects(project)
    .filter(sourceRoot => ScalaBackend.bySourceRoot(sourceRoot).isEmpty)
    .flatMap(sourceRoot => sharedBackends(sourceRoot).map(SharedProject(findSubproject(sourceRoot), sourceRoot, _)))

  // Verify that there are no duplicate backends among the shared projects.
  sharedProjects
    .groupBy(_.backends)
    .filterNot(_._2.size == 1)
    .foreach:
      case (backends: Set[ScalaBackend], projects: Set[SharedProject]) =>
        error(s"""duplicate shared directories for backends
               |${backends.map(_.sourceRoot).mkString(", ")}
               |${projects.map(_.sourceRoot).mkString(", ")}
               |""".stripMargin)
    
  private def sharedBackends(sourceRoot: String): Option[Set[ScalaBackend]] =
    import MixedProject.sharedSourceRoot
    def error(message: String): Nothing = this.error(s"shared source root '$sourceRoot': $message")

    Option.when(sourceRoot == sharedSourceRoot)(ScalaBackend.all).orElse:
      val suffix: String =
        if sourceRoot.startsWith(sharedSourceRoot)
        then Strings.drop(sourceRoot, s"$sharedSourceRoot-")
        else sourceRoot
      val backendNames: Array[String] = suffix.split("-", -1)
      val backendOpts: Set[Option[ScalaBackend]] = backendNames.toSet.map(ScalaBackend.bySourceRoot)
      Option.when(backendOpts.forall(_.isDefined)):
        val backends: Set[ScalaBackend] = backendOpts.map(_.get)
        if backends.size != backendNames.length then error("duplicate backend names")
        if backends.size == ScalaBackend.all.size then error(s"code shared between all the backends belongs in '$sharedSourceRoot'")
        if backends.size == 1 then
          val backend: ScalaBackend = backends.head
          error(s"code specific to the '${backend.name}' backend belongs in '${backend.sourceRoot}'")
        backends

  override def apply(): Unit =
    announce(s"using Scala backends ${backendProjects.map(_.backend.name).mkString(", ")}")

    // Apply plugin to subprojects.
    (backendProjects.map(_.project) ++ sharedProjects.map(_.project))
      .foreach(_.getPluginManager.apply(classOf[BackendPlugin]))

    // Disable tasks.
    Tasks.disable(project, classOf[SourceTask])
    Tasks.disable(project, classOf[AbstractArchiveTask])

    // Unregister Scala sources.
    Sources.removeAll(Configurations.mainSourceSet(project))
    Sources.removeAll(Configurations.testSourceSet(project))
