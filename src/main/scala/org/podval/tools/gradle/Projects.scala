package org.podval.tools.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.{Plugin, Project}
import org.gradle.api.internal.project.ProjectStateInternal
import java.io.File
import scala.jdk.CollectionConverters.SetHasAsScala

object Projects:
  def gradleUserHomeDir(project: Project): File = project.getGradle.getGradleUserHomeDir

  def applyPlugin[P <: Plugin[?]](project: Project, pluginClass: Class[P]): Unit = project
    .getPluginManager
    .apply(pluginClass)

  def parent(project: Project): Option[Project] = Option(project.getParent)

  def projectDir(project: Project): File = project.getProjectDir

  def isProjectDir(project: Project, projectDirName: String): Boolean =
    projectDir(project).getName == projectDirName

  def findProperty(project: Project, propertyName: String): Option[AnyRef] =
    Option(project.findProperty(propertyName))

  private def buildDirectoryProperty(project: Project): DirectoryProperty = project
    .getLayout
    .getBuildDirectory

  def buildDirectoryFile(project: Project): File = buildDirectoryProperty(project)
    .get
    .getAsFile

  def setBuildSubDirectory(project: Project, subDirectoryName: String): Unit = 
    val property: DirectoryProperty = buildDirectoryProperty(project)
    property.set(property.get.dir(subDirectoryName))

  def afterEvaluateIfAvailable(project: Project, action: => Unit): Unit =
    val state: ProjectStateInternal = project.getState.asInstanceOf[ProjectStateInternal]
    val isAfterEvaluateAvailable: Boolean = state.isUnconfigured || state.isConfiguring

    if isAfterEvaluateAvailable
    then project.afterEvaluate(_ => action)
    else action

  // We look up projects by their *directory* names, not by their *project* names,
  // so `Option(project.findProject(name))` does not do it for projects renamed in `settings.gradle` ;)
  def findSubproject(project: Project, name: String): Option[Project] = project
    .getSubprojects
    .asScala
    .find(isProjectDir(_, name))
