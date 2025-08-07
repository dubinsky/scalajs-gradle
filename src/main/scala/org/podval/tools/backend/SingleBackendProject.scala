package org.podval.tools.backend

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.podval.tools.build.ScalaBackend
import org.podval.tools.gradle.Sources
import org.podval.tools.platform.IntelliJIdea

class SingleBackendProject(
  project: Project,
  backend: ScalaBackend,
  jvmPluginServices: JvmPluginServices,
  sharedProjects: Set[SharedProject]
) extends SingleProject(project):
  override def apply(): Unit =
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

    setScalaVersionFromParentAndAddVersionSpecificSources()
    backend.apply(project, jvmPluginServices, isRunningInIntelliJ)
    backend.registerTasks(project)

    project.afterEvaluate: (_: Project) =>
      sharedProjects.map(_.project).foreach(Sources.addShared(_, project, isRunningInIntelliJ))
      backend.afterEvaluate(
        project,
        projectScalaLibrary = extension.getScalaLibrary,
        pluginScalaLibrary = extension.getPluginScalaLibrary
      )
