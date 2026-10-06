package org.podval.tools.backend

import org.gradle.api.initialization.{ProjectDescriptor, Settings}
import org.gradle.api.provider.Property
import javax.inject.Inject
import scala.jdk.CollectionConverters.SetHasAsScala

// Configured from settings.gradle. The project plugin reads `backend` later,
// because a value in build.gradle is too late: task classes are chosen in apply().
abstract class ScalaSettingsExtension @Inject(settings: Settings):
  def getBackend: Property[String]

  // Include js, jvm, native, shared, and partial-share directories under projects
  // that are already included. Call this after those includes.
  def includeBackendProjects(): Unit =
    projects(settings.getRootProject).foreach: (descriptor: ProjectDescriptor) =>
      BackendLayout.projectDirectories(descriptor.getProjectDir).foreach: (name: String) =>
        val path: String = ScalaSettingsExtension.childPath(descriptor.getPath, name)
        if settings.findProject(path) == null then settings.include(path)

  private def projects(descriptor: ProjectDescriptor): List[ProjectDescriptor] =
    descriptor :: descriptor.getChildren.asScala.toList.flatMap(projects)

object ScalaSettingsExtension:
  val gradleExtensionName: String = "scalaBackend"

  def childPath(parentPath: String, name: String): String =
    if parentPath == ":" then s":$name" else s"$parentPath:$name"
