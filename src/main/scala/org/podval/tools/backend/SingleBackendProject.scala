package org.podval.tools.backend

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.podval.tools.build.ScalaBackend
import org.podval.tools.gradle.Sources
import org.podval.tools.platform.IntelliJIdea

final class SingleBackendProject(
  project: Project,
  jvmPluginServices: JvmPluginServices,
  val backend: ScalaBackend,
  sharedProjects: Set[SharedProject]
) extends SingleProject(
  project
):
  override def announcement: String =
    val sharedString: String = if sharedProjects.isEmpty then "" else s" [+${WithProject.names(sharedProjects)}]"
    s"using Scala backend ${backend.name}$sharedString"

  private lazy val isRunningInIntelliJ: Boolean = IntelliJIdea.runningIn

  override def apply(): Unit =
    // Create extension.
    BackendExtension.create(project, backend, isRunningInIntelliJ)
    
    backend.apply(project, jvmPluginServices, isRunningInIntelliJ)
    backend.registerTasks(project)
  
  override def afterEvaluate(): Unit =
    SingleProject.forEach(sharedProjects, Sources.addShared(_, project, isRunningInIntelliJ))

    val extension: BackendExtension = BackendExtension.get(project)
    
    backend.afterEvaluate(
      project,
      projectScalaLibrary = extension.getScalaLibrary,
      pluginScalaLibrary  = extension.getPluginScalaLibrary
    )
    