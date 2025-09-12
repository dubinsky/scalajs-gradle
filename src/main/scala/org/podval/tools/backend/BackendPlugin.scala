package org.podval.tools.backend

import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.{Plugin, Project}
import org.podval.tools.build.ScalaBackend
import org.podval.tools.gradle.Projects
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.platform.IntelliJIdea
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
          .flatMap(_.subProjects.find(_.is(project)))
      .getOrElse:
        SingleBackendProject(
          project,
          jvmPluginServices,
          sharedProjects = Set.empty,
          backend = Projects
            .findProperty(project, ScalaBackend.property)
            .map: (backendName: String) =>
              ScalaBackend
                .byName(backendName)
                .getOrElse(error(ScalaBackend.unknownBackendMessage(backendName)))
            .getOrElse:
              info(ScalaBackend.noPropertyMessage)
              JvmBackend
        )

    // Announce the project.
    info(s"${backendProject.announcement}${if !IntelliJIdea.runningIn then "" else " [IJ]"}")

    // Configure the project.
    
    // Note: register `afterEvaluate()` *before* calling `apply()`,
    // so that `afterEvaluate()` registered by `apply()` (if any)
    // can rely on the Scala version being already set,
    // and version-specific sources being already added, etc.
    backendProject.project.afterEvaluate(_ => backendProject.afterEvaluate())
    
    backendProject.apply()
