package org.podval.tools.backend

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.podval.tools.util.{Extensions, Projects}

// Created on a mixed project, under the same extension name as BackendExtension.
// A mixed project has no single backend, so it does not get BackendExtension.
// Backend projects inherit this value unless their own build script sets one.
abstract class UseArtifactSuffix:
  def getUseArtifactSuffix: Property[Boolean]

object UseArtifactSuffix:
  def create(project: Project): UseArtifactSuffix =
    val extension: UseArtifactSuffix = Extensions.create(
      project,
      BackendExtension.extensionName,
      classOf[UseArtifactSuffix]
    )
    configure(extension.getUseArtifactSuffix)
    extension

  def configure(property: Property[Boolean]): Unit =
    property.convention(true)
    property.finalizeValueOnRead()

  // convention() replaces the default and leaves an explicit value in place,
  // so a backend project's own assignment wins over the mixed project.
  // The jar step calls this, then reads the property (finalizeValueOnRead).
  // The publication step calls it again. A second convention() throws once the value is final,
  // so the extra key makes that call a no-op. Do not .get the parent here.
  def inherit(project: Project, own: Property[Boolean]): Unit =
    val key: String = "org.podval.tools.useArtifactSuffix.inherited"
    val extra = project.getExtensions.getExtraProperties
    if extra.has(key) then ()
    else
      Projects.parent(project).foreach: parent =>
        Extensions.findByType(parent, classOf[UseArtifactSuffix]).foreach: parentExtension =>
          own.convention(parentExtension.getUseArtifactSuffix)
      extra.set(key, java.lang.Boolean.TRUE)
