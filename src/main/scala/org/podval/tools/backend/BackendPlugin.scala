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
import javax.inject.Inject

final class BackendPlugin @Inject(jvmPluginServices: JvmPluginServices) extends Plugin[Project]:
  import MixedProject.SharedProject

  override def apply(project: Project): Unit =
    def announce(message: String): Unit = BackendPlugin.announce(project, message)

    // Apply Scala plugin to this project.
    Projects.applyPlugin(project, classOf[ScalaPlugin])

    // Configure project.
    MixedProject(project) match
      // Mixed
      case Some(mixedProject: MixedProject) =>
        announce(s"using Scala backends ${mixedProject.backendNames.mkString(", ")}")
        // Apply plugin to subprojects.
        mixedProject.projects.foreach(_.getPluginManager.apply(classOf[BackendPlugin]))
        // Disable tasks.
        Tasks.disable(project, classOf[SourceTask])
        Tasks.disable(project, classOf[AbstractArchiveTask])
        // Unregister Scala sources.
        Sources.removeAll(Configurations.mainSourceSet(project))
        Sources.removeAll(Configurations.testSourceSet(project))

      case None =>
        val parent: Option[MixedProject] = Projects.parent(project).flatMap(MixedProject.apply)
        parent.flatMap(_.sharedProject(project)) match
          // Shared
          case Some(sharedProjectThis: SharedProject) =>
            val sharedString: String = sharedProjectThis.backends.map(_.name).mkString(", ")
            announce(s"code shared between backends $sharedString")
            BackendPlugin.setScalaVersionFromParentAndAddVersionSpecificSources(project)
            // Disable all tasks.
            Tasks.disable(project, classOf[Task])

          // Single Backend
          case None =>
            val backend: ScalaBackend = BackendPlugin.backend(project)
            val sharedProjects: Set[SharedProject] = parent.toSet.flatMap(_.sharedProjectsForBackend(backend))
            val sharedString: String =
              if sharedProjects.isEmpty
              then ""
              else sharedProjects.map(_.sourceRoot).mkString(" [+ ", ", ", "]")
            announce(s"using Scala backend ${backend.name}$sharedString")

            val isRunningInIntelliJ: Boolean = IntelliJIdea.runningIn

            // Create extension.
            val extension: BackendExtension = project.getExtensions.create(
              "scalaBackend",
              classOf[BackendExtension],
              backend,
              isRunningInIntelliJ
            )

            BackendPlugin.setScalaVersionFromParentAndAddVersionSpecificSources(project)
            backend.apply(project, jvmPluginServices, isRunningInIntelliJ)
            backend.registerTasks(project)

            project.afterEvaluate: (_: Project) =>
              sharedProjects.map(_.project).foreach(Sources.addShared(_, project, isRunningInIntelliJ))
              backend.afterEvaluate(
                project,
                projectScalaLibrary = extension.getScalaLibrary,
                pluginScalaLibrary = extension.getPluginScalaLibrary
              )

object BackendPlugin:
  private def setScalaVersionFromParentAndAddVersionSpecificSources(project: Project): Unit =
    Projects.parent(project).foreach: (parentProject: Project) =>
      Projects.afterEvaluateIfAvailable(parentProject, ScalaExtension
        .findScalaVersion(parentProject)
        .foreach: (scalaVersion: ScalaVersion) =>
          ScalaExtension.setScalaVersion(project, scalaVersion)
          Sources.addVersionSpecific(project, scalaVersion)
      )

  val scalaBackendProperty: String = "org.podval.tools.backend"

  def error(project: Project, message: String): Nothing =
    throw GradleException(s"${pluginMessage(project, message)}\nDocumentation: https://github.com/dubinsky/scalajs-gradle")

  private def info(project: Project, message: String): Unit =
    project.getLogger.info(pluginMessage(project, message), null, null, null)

  private def announce(project: Project, message: String): Unit =
    val isRunningInIntelliJ: Boolean = IntelliJIdea.runningIn
    val ijString: String = if !isRunningInIntelliJ then "" else " [IJ]"
    info(project, s"message$ijString")

  private final def pluginMessage(project: Project, message: String): String =
    s"Plugin 'org.podval.tools.scalajs' in $project: $message."

  private def backendNames: String = ScalaBackend.all.map(backend => s"${backend.name} (${backend.sourceRoot})").mkString(", ")
  private def backendSourceRoots: String = ScalaBackend.all.map(_.sourceRoot).mkString(", ")

  private def backend(project: Project): ScalaBackend = ScalaBackend
    .bySourceRoot(Projects.projectDir(project).getName)
    .orElse:
      Projects
        .findProperty(project, scalaBackendProperty)
        .map(_.toString)
        .map((name: String) => ScalaBackend
          .byAnyName(name)
          .getOrElse(error(project, s"unknown Scala backend '$name'; use one of $backendNames"))
        )
    .getOrElse:
      info(project,
        s"""to choose Scala backend, set property '$scalaBackendProperty' to one of $backendNames;
           |to use multiple backends, create at least one of the subprojects $backendSourceRoots""".stripMargin
      )
      JvmBackend
