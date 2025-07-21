package org.podval.tools.backend

import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.{GradleException, Plugin, Project, Task}
import org.podval.tools.build.{ScalaBackend, ScalaVersion}
import org.podval.tools.gradle.{Configurations, Projects, ScalaExtension, Sources, Tasks}
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.platform.IntelliJIdea
import java.io.File
import javax.inject.Inject

final class BackendPlugin @Inject(jvmPluginServices: JvmPluginServices) extends Plugin[Project]:
  override def apply(project: Project): Unit =
    def info(message: String): Unit = BackendPlugin.info(project, message)

    // Apply Scala plugin to this project.
    Projects.applyPlugin(project, classOf[ScalaPlugin])

    // Configure project.
    val isRunningInIntelliJ: Boolean = IntelliJIdea.runningIn
    val (backends: Set[ScalaBackend], subprojects: Set[Project]) = BackendPlugin.subprojects(project)
    val isMixed: Boolean = backends.nonEmpty
    val ijString: String = if !isRunningInIntelliJ then "" else " [IJ]"
    val shared: Option[Project] = Option(if isMixed then project else project.getParent).flatMap(BackendPlugin.shared)

    if isMixed then
      info(s"using Scala backends ${backends.map(_.name).mkString(", ")}$ijString")
      BackendPlugin.applyMixed(project, subprojects, shared)
    
    else if shared.isDefined && Projects.isProjectDir(project, ScalaBackend.sharedSourceRoot) then
      info(s"code shared between backends$ijString")
      BackendPlugin.applyShared(project)

    else
      val backend: ScalaBackend = BackendPlugin.backend(project)
      info(s"using Scala backend ${backend.name}${shared.map(shared => s" [+'${shared.getName}']").getOrElse("")}$ijString")
      BackendPlugin.applySingle(project, jvmPluginServices, isRunningInIntelliJ, backend, shared)

object BackendPlugin:
  val scalaBackendProperty: String = "org.podval.tools.backend"

  def error(project: Project, message: String): Nothing =
    throw GradleException(s"${pluginMessage(project, message)}\nDocumentation: https://github.com/dubinsky/scalajs-gradle")

  private def info(project: Project, message: String): Unit =
    project.getLogger.info(pluginMessage(project, message), null, null, null)

  private final def pluginMessage(project: Project, message: String): String =
    s"Plugin 'org.podval.tools.scalajs' in $project: $message."

  private def subprojects(project: Project): (Set[ScalaBackend], Set[Project]) =
    val backends: Set[ScalaBackend] = ScalaBackend.all.filter(backend => project.file(backend.sourceRoot).exists)
    if backends.isEmpty then (Set.empty, Set.empty) else
      val backendProjects: Set[(ScalaBackend, Option[Project])] = backends
        .map(backend => backend -> Projects.findSubproject(project, backend.sourceRoot))

      val notSubprojects: Set[ScalaBackend] = backendProjects.filter(_._2.isEmpty).map(_._1)
      if notSubprojects.nonEmpty then error(project,
        s"${notSubprojects.map(_.sourceRoot).map(n => s"'$n'").mkString(", ")} must be included in 'settings.gradle'"
      )

      (backends, backendProjects.map(_._2.get))

  private def shared(parentProject: Project): Option[Project] =
    val file: File = parentProject.file(ScalaBackend.sharedSourceRoot)
    if !file.exists || !file.isDirectory then None else Some(
      Projects.findSubproject(parentProject, ScalaBackend.sharedSourceRoot)
        .getOrElse(error(parentProject, s"subproject '${ScalaBackend.sharedSourceRoot}' must be included in 'settings.gradle'"))
    )

  private def backend(project: Project): ScalaBackend = ScalaBackend
    .all
    .find(_.sourceRoot == Projects.projectDir(project).getName)
    .orElse:
      Projects
        .findProperty(project, scalaBackendProperty)
        .map(_.toString)
        .map((name: String) => ScalaBackend
          .all
          .find(isBackendWithName(name))
          .getOrElse(error(project, s"unknown Scala backend '$name'; use one of ${ScalaBackend.names}"))
        )
    .getOrElse:
      info(project,
        s"""to choose Scala backend, set property '$scalaBackendProperty' to one of ${ScalaBackend.names};
           |to use multiple backends, create at least one of the subprojects ${ScalaBackend.sourceRoots}""".stripMargin
      )
      JvmBackend

  private def isBackendWithName(name: String)(backend: ScalaBackend): Boolean =
    name.toLowerCase == backend.name      .toLowerCase ||
    name.toLowerCase == backend.sourceRoot.toLowerCase

  private def applyMixed(
    project: Project,
    subprojects: Set[Project],
    shared: Option[Project]
  ): Unit =
    // Apply plugin to subprojects.
    (subprojects ++ shared.toSeq).foreach(_.getPluginManager.apply(classOf[BackendPlugin]))

    // Disable tasks.
    Tasks.disable(project, classOf[SourceTask])
    Tasks.disable(project, classOf[AbstractArchiveTask])

    // Unregister Scala sources.
    Sources.removeAll(Configurations.mainSourceSet(project))
    Sources.removeAll(Configurations.testSourceSet(project))

  private def applySingle(
    project: Project,
    jvmPluginServices: JvmPluginServices,
    isRunningInIntelliJ: Boolean,
    backend: ScalaBackend,
    shared: Option[Project]
  ): Unit =
    // Create extension.
    val extension: BackendExtension = project.getExtensions.create(
      "scalaBackend",
      classOf[BackendExtension],
      backend,
      isRunningInIntelliJ
    )

    setScalaVersionFromParentAndAddVersionSpecificSources(project)
    backend.apply(project, jvmPluginServices, isRunningInIntelliJ)
    backend.registerTasks(project)

    project.afterEvaluate: (_: Project) =>
      shared.foreach(Sources.addShared(_, project, isRunningInIntelliJ))
      backend.afterEvaluate(
        project, 
        projectScalaLibrary = extension.getScalaLibrary, 
        pluginScalaLibrary = extension.getPluginScalaLibrary
      )

  private def applyShared(project: Project): Unit =
    setScalaVersionFromParentAndAddVersionSpecificSources(project)

    // Disable all tasks.
    Tasks.disable(project, classOf[Task])

  private def setScalaVersionFromParentAndAddVersionSpecificSources(project: Project): Unit =
    Projects.parent(project).foreach: (parentProject: Project) =>
      Projects.afterEvaluateIfAvailable(parentProject, ScalaExtension
        .findScalaVersion(parentProject)
        .foreach: (scalaVersion: ScalaVersion) =>
          ScalaExtension.setScalaVersion(project, scalaVersion)
          Sources.addVersionSpecific(project, scalaVersion)
      )
