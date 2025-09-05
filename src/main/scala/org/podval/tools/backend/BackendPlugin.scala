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
    MixedProject(project)
      .orElse:
        Projects
          .parent(project)
          .flatMap(MixedProject(_))
          .flatMap(_.findProject(project, jvmPluginServices))
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
      .apply()
