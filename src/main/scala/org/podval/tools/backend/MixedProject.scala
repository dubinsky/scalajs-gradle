package org.podval.tools.backend

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.podval.tools.build.{ScalaBackend, ScalaVersion}
import org.podval.tools.gradle.{Projects, Sources, Tasks}
import org.podval.tools.platform.Strings

object MixedProject:
  private val sharedSourceRoot: String = "shared"

  private def backendBySourceRoot(sourceRoot: String): Option[ScalaBackend] = ScalaBackend
    .all
    .find(_.sourceRoot == sourceRoot)

  def apply(
    project: Project,
    jvmPluginServices: JvmPluginServices
  ): Option[MixedProject] =
    val (
      withBackend   : Set[(String, Option[ScalaBackend])],
      withoutBackend: Set[(String, Option[ScalaBackend])]
    ) = Projects
      .subProjects(project)
      .map(sourceRoot => sourceRoot -> backendBySourceRoot(sourceRoot))
      .partition(_._2.isDefined)

    Option.when(withBackend.nonEmpty)(new MixedProject(
      project,
      jvmPluginServices,
      backends = withBackend.map(_._2.get),
      sharedSourceRoots = withoutBackend.map(_._1)
    ))

final class MixedProject private(
  project: Project,
  jvmPluginServices: JvmPluginServices,
  backends: Set[ScalaBackend],
  sharedSourceRoots: Set[String]
) extends BackendProject(project):
  require(backends.nonEmpty)

  private lazy val sharedSelf: Option[SharedProject] = Option.when(Sources.exist(project))(SharedProject(
    project,
    ScalaBackend.all
  ))

  private val sharedProjects: Set[SharedProject] = sharedSourceRoots.flatMap: (sourceRoot: String) =>
    import MixedProject.sharedSourceRoot
    Option
      .when(sourceRoot == sharedSourceRoot):
        ScalaBackend.all
      .orElse:
        def error(message: String): Nothing = this.error(s"shared source root '$sourceRoot': $message")
        val backendNames: Array[String] = Strings.dropPrefixIfPresent(sourceRoot, s"$sharedSourceRoot-").split("-", -1)
        val backendsSeq: Seq[ScalaBackend] = backendNames.toSeq.flatMap(MixedProject.backendBySourceRoot)
        Option.when(backendsSeq.length == backendNames.length):
          val backends: Set[ScalaBackend] = backendsSeq.toSet
          if backends.size != backendNames.length then error(s"duplicate backend names")
          if backends.size == ScalaBackend.all.size then error(s"code shared between all the backends belongs in '$sharedSourceRoot'")
          if backends.size == 1 then error(s"code specific to the '${backends.head.name}' backend belongs in '${backends.head.sourceRoot}'")
          backends
      .map:
        SharedProject(
          project = findSubProject(sourceRoot),
          _
        )

  // Verify that there are no duplicate backends among the shared projects.
  sharedProjects
    .groupBy(_.backends)
    .filterNot(_._2.size == 1)
    .foreach:
      case (backends: Set[ScalaBackend], projects: Set[SharedProject]) =>
        error(s"""duplicate shared directories for backends
               |${Strings.toString(backends, _.sourceRoot)}
               |${Strings.toString(projects, _.name)}
               |""".stripMargin)

  private val singleBackendProjects: Set[SingleBackendProject] = backends.map: (backend: ScalaBackend) =>
    SingleBackendProject(
      project = findSubProject(backend.sourceRoot),
      jvmPluginServices,
      backend,
      sharedProjects = (sharedProjects ++ sharedSelf.toSet).filter(_.backends.contains(backend))
    )

  private def findSubProject(sourceRoot: String): Project = Projects
    .findSubProject(project, sourceRoot)
    .getOrElse(error(s"subproject '$sourceRoot' must be included in 'settings.gradle'"))

  private def subProjects: Set[SingleProject] = singleBackendProjects ++ sharedProjects

  def findSubProject(project: Project): Option[SingleProject] = subProjects.find(_.is(project))
  
  override def announcement: String = s"using Scala backends ${Strings.toString(backends, _.name)}"

  override def apply(): Unit =
    // Disable tasks.
    Set(
      classOf[SourceTask],
      classOf[AbstractArchiveTask]
    )
      .foreach(Tasks.disable(project, _))

    // Apply plugin to subprojects.
    subProjects.map(_.project).foreach(Projects.applyPlugin(_, classOf[BackendPlugin]))

  override def afterEvaluate(): Unit =
    val scalaVersion: ScalaVersion = getScalaVersionFromScalaExtension
    subProjects.foreach(_.setScalaVersion(scalaVersion))
    (subProjects ++ sharedSelf.toSet).foreach(_.addVersionSpecificSources(scalaVersion))
