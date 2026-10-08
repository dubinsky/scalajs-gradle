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

    // Publication ids only. maven-publish can still setArtifactId from afterEvaluate.
    // Jar names are not applied here: Gradle 9.8.1 freezes a project-dependency File
    // before this listener runs.
    project.getGradle.projectsEvaluated(_ => configurePublishedCoordinates())
  
  override def afterEvaluate(): Unit =
    // Next NotifyAfterEvaluate batch: after a build-script afterEvaluate on this project,
    // and after the mixed parent (configured first) has assigned useArtifactSuffix.
    // Before state.configured(), so a project dependency freezes the suffixed jar name.
    project.afterEvaluate(_ => configureJarArtifacts())

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
      pluginScalaLibrary  = extension.getPluginScalaLibrary
    )

  private def configureJarArtifacts(): Unit =
    val extension: BackendExtension = BackendExtension.get(project)
    UseArtifactSuffix.inherit(project, extension.getUseArtifactSuffix)
    val suffix: String = backend.jarArtifactSuffix(
      extension.getScalaLibrary,
      useArtifactSuffix = extension.getUseArtifactSuffix.get
    )
    backend.configureJarArtifacts(project, suffix)

  private def configurePublishedCoordinates(): Unit =
    val extension: BackendExtension = BackendExtension.get(project)
    // inherit is a no-op if the jar step already ran. Do not configure jars here.
    UseArtifactSuffix.inherit(project, extension.getUseArtifactSuffix)
    val suffix: String = backend.jarArtifactSuffix(
      extension.getScalaLibrary,
      useArtifactSuffix = extension.getUseArtifactSuffix.get
    )
    backend.configurePublishedCoordinates(project, suffix)

  private def addSharedSources(shared: Project): Unit =
    def add(): Unit = addSources: (sourceSetGetter, _, directorySetGetter) =>
      IntelliJSharedSources.directories(directorySetGetter(sourceSetGetter(shared)))

    if !isRunningInIntelliJ then add() else IntelliJSharedSources.defer(project, shared, add)
