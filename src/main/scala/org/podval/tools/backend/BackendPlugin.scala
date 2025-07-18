package org.podval.tools.backend

import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.{Plugin, Project, Task}
import org.podval.tools.build.{ScalaBackend, ScalaVersion}
import org.podval.tools.gradle.{Configurations, Projects, ScalaExtension, Sources, Tasks}
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.util.Files
import java.io.File
import javax.inject.Inject

final class BackendPlugin @Inject(jvmPluginServices: JvmPluginServices) extends Plugin[Project]:
  override def apply(project: Project): Unit =
    // Apply Scala plugin to this project.
    Projects.applyPlugin(project, classOf[ScalaPlugin])

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
    
    else if shared.isDefined &&  Projects.isProjectDir(project, ScalaBackend.sharedSourceRoot) then
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
        file = File(Projects.projectDir(project), "settings-includes.gradle"),
        content = (Seq(ScalaBackend.sharedSourceRoot) ++ backends.map(_.sourceRoot))
          .map(name => s"include '$prefix:$name'")
          .mkString("\n")
      )
    val backendProjects: Set[(ScalaBackend, Option[Project])] = backends
      .map(backend => backend -> Projects.findSubproject(project, backend.sourceRoot))

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
      Projects.findSubproject(parentProject, ScalaBackend.sharedSourceRoot)
        .getOrElse(extension.error(s"subproject '${ScalaBackend.sharedSourceRoot}' must be included in 'settings.gradle'"))
    )

  private def backend(
    project: Project,
    extension: BackendExtension
  ): ScalaBackend = ScalaBackend
    .all
    .find(_.sourceRoot == Projects.projectDir(project).getName)
    .orElse:
      Projects
        .findProperty(project, scalaBackendProperty)
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
    Tasks.disable(project, classOf[Task])
  
  private def applyMixed(
    project: Project,
    extension: BackendExtension,
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
    extension: BackendExtension,
    jvmPluginServices: JvmPluginServices,
    backend: ScalaBackend
  ): Unit =
    setScalaVersionFromParentAndAddVersionSpecificScalaSources(project, extension)
    extension.setBackend(backend)
    backend.apply(project, jvmPluginServices, extension.isRunningInIntelliJ)
    backend.registerTasks(project)

  private def afterEvaluateSingle(
    project: Project,
    extension: BackendExtension,
    shared: Option[Project],
    backend: ScalaBackend
  ): Unit =
    shared.foreach((shared: Project) => Sources.addShared(
      project,
      shared,
      extension.isRunningInIntelliJ
    ))

    // Adjust the build directory for the Scala version if requested.
    if extension.isBuildPerScalaVersion
    then Projects.setBuildSubDirectory(project, s"scala-${extension.getScalaVersion}")

    backend.afterEvaluate(project, extension.getScalaLibrary)

  private def setScalaVersionFromParentAndAddVersionSpecificScalaSources(
    project: Project,
    extension: BackendExtension
  ): Unit = Projects.parent(project).foreach: (parentProject: Project) =>
    Projects.afterEvaluateIfAvailable(parentProject, ScalaExtension
      .findScalaVersion(parentProject)
      .foreach: (scalaVersion: ScalaVersion) =>
        ScalaExtension.setScalaVersion(project, scalaVersion)
        Sources.addVersionSpecific(project, scalaVersion)
    )
