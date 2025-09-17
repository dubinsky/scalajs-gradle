package org.podval.tools.backend

import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.{Plugin, Project}
import org.podval.tools.build.ScalaBackend
import org.podval.tools.gradle.Projects
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.platform.{IntelliJIdea, Strings}
import javax.inject.Inject

final class BackendPlugin @Inject(
  project: Project,
  jvmPluginServices: JvmPluginServices
) extends WithProject(project) with Plugin[Project]:

  override def apply(project: Project): Unit =
    // Apply Scala plugin to this project.
    Projects.applyPlugin(project, classOf[ScalaPlugin])

    // Determine what type of BackendProject we have.
    val backendProject: BackendProject = MixedProject(project, jvmPluginServices)
      .orElse:
        Projects
          .parent(project)
          .flatMap(MixedProject(_, jvmPluginServices))
          .flatMap(_.findSubProject(project))
      .getOrElse:
        SingleBackendProject(
          project,
          jvmPluginServices,
          sharedProjects = Set.empty,
          backend = getBackend
        )

    // Announce the project.
    info(s"${backendProject.announcement}${if !IntelliJIdea.runningIn then "" else " [IJ]"}")

    // Configure the project.
    
    // Note: register `afterEvaluate()` *before* calling `apply()`,
    // so that `afterEvaluate()` registered by `apply()` (if any)
    // can rely on the Scala version being already set,
    // version-specific sources being already added, etc.
    backendProject.project.afterEvaluate(_ => backendProject.afterEvaluate())
    
    backendProject.apply()

  private def getBackend: ScalaBackend = Projects
    .findProperty(project, ScalaBackend.property)
    .map: (backendName: String) =>
      ScalaBackend
        .all
        .find((backend: ScalaBackend) =>
          backendName.toLowerCase == backend.name      .toLowerCase ||
          backendName.toLowerCase == backend.sourceRoot.toLowerCase
        )
        .getOrElse:
          error(mkMessage(s"unknown Scala backend '$backendName'"))
    .getOrElse:
      info(mkMessage("backend not set"))
      JvmBackend

  private def mkMessage(message: String): String =
    s"""$message;
       |to use one backend, set property '${ScalaBackend.property}' to one of
       |${Strings.toString(ScalaBackend.all, _.fullName)};
       |to use multiple backends, create at least one of the subprojects
       |${Strings.toString(ScalaBackend.all, _.sourceRoot)}""".stripMargin
