package org.podval.tools.backend

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.project.ProjectStateInternal
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.plugins.scala.{ScalaPlugin, ScalaPluginExtension}
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.{Plugin, Project, Task}
import org.podval.tools.build.{ScalaBackend, ScalaVersion, SourceSets}
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.util.Files
import scala.jdk.CollectionConverters.SetHasAsScala
import java.io.File
import javax.inject.Inject

final class BackendPlugin @Inject(jvmPluginServices: JvmPluginServices) extends Plugin[Project]:
  override def apply(project: Project): Unit =
    // Apply Scala plugin to this project.
    project.getPluginManager.apply(classOf[ScalaPlugin])

    // Create extension.
    val extension: BackendExtension = project.getExtensions.create("scalaBackend", classOf[BackendExtension])

    // Configure project.
    val subprojects: Set[(ScalaBackend, Option[Project])] = BackendPlugin.subprojects(project, extension)
    val isMixed: Boolean = subprojects.nonEmpty
    val ijString: String = if !extension.isRunningInIntelliJ then "" else " [IJ]"
    val shared: Option[Project] = (if isMixed then Some(project) else Option(project.getParent))
      .flatMap(BackendPlugin.shared(_, extension))

    if isMixed then
      extension.lifecycle(s"using Scala backends ${subprojects.map(_._1.name).mkString(", ")}$ijString")
      BackendPlugin.applyMixed(
        project,
        extension,
        subprojects.map(_._2.get),
        shared
      )
    
    else if shared.isDefined && project.getProjectDir.getName == ScalaBackend.sharedSourceRoot then
      extension.lifecycle(s"code shared between backends$ijString")
      BackendPlugin.applyShared(
        project,
        extension
      )
    
    else
      val backend: ScalaBackend = BackendPlugin.backend(project, extension)
      extension.lifecycle(s"using Scala backend ${backend.name}${shared.map(shared => s" [+'${shared.getName}']").getOrElse("")}$ijString")
      project.afterEvaluate((_: Project) => BackendPlugin.afterEvaluateSingle(
        project,
        extension,
        shared,
        backend
      ))
      BackendPlugin.applySingle(
        project, 
        extension,
        jvmPluginServices,
        backend
      )

object BackendPlugin:
  val scalaBackendProperty: String = "org.podval.tools.backend"
  val buildPerScalaVersionProperty: String = "org.podval.tools.backend.buildPerScalaVersion"

  private def subprojects(
    project: Project,
    extension: BackendExtension
  ): Set[(ScalaBackend, Option[Project])] =
    val backends: Set[ScalaBackend] = ScalaBackend.all.filter(backend => project.file(backend.sourceRoot).exists)
    // Write `settings-includes.gradle`.
    if backends.nonEmpty then
      val path: String = project.getPath
      val prefix: String = if path == ":" then "" else path
      Files.write(
        file = File(project.getProjectDir, "settings-includes.gradle"),
        content = (Seq(ScalaBackend.sharedSourceRoot) ++ backends.map(_.sourceRoot))
          .map(name => s"include '$prefix:$name'")
          .mkString("\n")
      )
    val backendProjects: Set[(ScalaBackend, Option[Project])] = backends
      .map(backend => backend -> findSubproject(project, backend.sourceRoot))

    val notSubprojects: Set[ScalaBackend] = backendProjects.filter(_._2.isEmpty).map(_._1)
    if notSubprojects.nonEmpty then extension.error(
      s"${notSubprojects.map(_.sourceRoot).map(n => s"'$n'").mkString(", ")} must be included in 'settings.gradle'"
    )
    
    backendProjects

  private def shared(
    parentProject: Project,
    extension: BackendExtension
  ): Option[Project] =
    val file: File = parentProject.file(ScalaBackend.sharedSourceRoot)
    if !file.exists || !file.isDirectory then None else Some(
      findSubproject(parentProject, ScalaBackend.sharedSourceRoot)
        .getOrElse(extension.error(s"subproject '${ScalaBackend.sharedSourceRoot}' must be included in 'settings.gradle'"))
    )

  // We look up projects by their *directory* names, not by their *project* names,
  // so `Option(project.findProject(name))` does not do it for projects renamed in `settings.gradle` ;)
  private def findSubproject(project: Project, name: String): Option[Project] = project
    .getSubprojects
    .asScala
    .find(_.getProjectDir.getName == name)

  private def backend(
    project: Project,
    extension: BackendExtension
  ): ScalaBackend = ScalaBackend
    .all
    .find(_.sourceRoot == project.getProjectDir.getName)
    .orElse:
      Option(project.findProperty(scalaBackendProperty))
        .map(_.toString)
        .map((name: String) => ScalaBackend
          .all
          .find(isBackendWithName(name))
          .getOrElse(extension.error(s"unknown Scala backend '$name'; use one of ${ScalaBackend.names}"))
        )
    .getOrElse:
      extension.lifecycle(
        s"""to choose Scala backend, set property '$scalaBackendProperty' to one of ${ScalaBackend.names};
           |to use multiple backends, create at least one of the subprojects ${ScalaBackend.sourceRoots}""".stripMargin
      )
      JvmBackend

  private def isBackendWithName(name: String)(backend: ScalaBackend): Boolean =
    name.toLowerCase == backend.name      .toLowerCase ||
    name.toLowerCase == backend.sourceRoot.toLowerCase

  private def applyShared(
    project: Project,
    extension: BackendExtension
  ): Unit =
    setScalaVersionFromParentAndAddVersionSpecificScalaSources(project, extension)

    // Disable all tasks.
    disableTasks(project, classOf[Task])
  
  private def applyMixed(
    project: Project,
    extension: BackendExtension,
    subprojects: Set[Project],
    shared: Option[Project]
  ): Unit =
    // Apply plugin to subprojects.
    (subprojects ++ shared.toSeq).foreach(_.getPluginManager.apply(classOf[BackendPlugin]))

    // Disable tasks.
    disableTasks(project, classOf[SourceTask])
    disableTasks(project, classOf[AbstractArchiveTask])

    // Unregister Scala sources.
    Sources.clearSourceSet(SourceSets.mainSourceSet(project))
    Sources.clearSourceSet(SourceSets.testSourceSet(project))

  private def applySingle(
    project: Project,
    extension: BackendExtension,
    jvmPluginServices: JvmPluginServices,
    backend: ScalaBackend
  ): Unit =
    setScalaVersionFromParentAndAddVersionSpecificScalaSources(project, extension)
    extension.setBackend(backend)

    backend.apply(project, jvmPluginServices)
    backend.registerTasks(project)

  private def afterEvaluateSingle(
    project: Project,
    extension: BackendExtension,
    shared: Option[Project],
    backend: ScalaBackend
  ): Unit =
    shared.foreach((shared: Project) => Sources.addSharedSources(
      project,
      shared,
      extension.isRunningInIntelliJ
    ))

    // Adjust the build directory for the Scala version if requested.
    if extension.isBuildPerScalaVersion then
      val buildDirectory: DirectoryProperty = project.getLayout.getBuildDirectory
      buildDirectory.set(buildDirectory.get.dir(s"scala-${extension.getScalaVersion}"))

    backend.afterEvaluate(project, extension.getScalaLibrary)

  private def disableTasks(project: Project, clazz: Class[? <: Task]): Unit =
    project.getTasks.withType(clazz).configureEach(_.setEnabled(false))

  private def setScalaVersionFromParentAndAddVersionSpecificScalaSources(
    project: Project,
    extension: BackendExtension
  ): Unit = Option(project.getParent).foreach((parentProject: Project) =>
    def set(): Unit = Option(parentProject.getExtensions.findByType(classOf[ScalaPluginExtension]))
      .flatMap(scalaPluginExtension => Option(scalaPluginExtension.getScalaVersion.getOrNull).map(ScalaVersion(_)))
      .foreach((scalaVersion: ScalaVersion) =>
        extension.getScalaExtensionScalaVersionProperty.set(scalaVersion.toString)
        Sources.addVersionSpecificScalaSources(project, scalaVersion)
      )

    val state: ProjectStateInternal = parentProject.getState.asInstanceOf[ProjectStateInternal]
    if state.isUnconfigured || state.isConfiguring
    then parentProject.afterEvaluate(_ => set())
    else set()
  )
