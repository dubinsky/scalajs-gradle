package org.podval.tools.backend

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.podval.tools.build.{Backend, ScalaBinaryVersion}
import org.podval.tools.nonjvm.ScalaCompiles
import org.podval.tools.util.Strings

final class SingleBackendProject(
  project: Project,
  jvmPluginServices: JvmPluginServices,
  backend: Backend,
  sharedProjects: Set[SharedProject]
) extends SingleProject(
  project
):
  override def announcement: String =
    val sharedString: String = if sharedProjects.isEmpty then "" else s" [+${Strings.toString(sharedProjects, _.name)}]"
    s"using Scala backend ${backend.name}$sharedString"

  private lazy val isRunningInIntelliJ: Boolean = IntelliJIdea.runningIn

  override def apply(): Unit =
    // Create extension.
    BackendExtension.create(project, backend, isRunningInIntelliJ)
    
    // Apply the backend.
    backend.apply(project, jvmPluginServices, isRunningInIntelliJ)
    backend.registerTasks(project)
  
  override def afterEvaluate(): Unit =
    sharedProjects.map(_.project).foreach(addSharedSources)

    val extension: BackendExtension = BackendExtension.get(project)
    
    val projectScalaLibrary = extension.getScalaLibrary
    ScalaCompiles.configureJavaRelease(
      project,
      scala3LibraryCompiledByScala3 = projectScalaLibrary.scalaVersion.binaryVersion match
        case ScalaBinaryVersion.Scala3WithScala3Library => true
        case _ => false
    )
    backend.afterEvaluate(
      project,
      projectScalaLibrary = projectScalaLibrary,
      pluginScalaLibrary  = extension.getPluginScalaLibrary,
      useArtifactSuffix   = extension.getUseArtifactSuffix.get
    )

  private def addSharedSources(shared: Project): Unit =
    def add(): Unit = addSources: (sourceSetGetter, _, directorySetGetter) =>
      IntelliJSharedSources.directories(directorySetGetter(sourceSetGetter(shared)))

    if !isRunningInIntelliJ then add() else IntelliJSharedSources.defer(project, shared, add)
