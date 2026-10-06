package org.podval.tools.backend

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.podval.tools.util.Extensions

final class ScalaSettingsPlugin extends Plugin[Settings]:
  override def apply(settings: Settings): Unit =
    val extension: ScalaSettingsExtension = Extensions.create(
      extensionAware = settings,
      name = ScalaSettingsExtension.gradleExtensionName,
      clazz = classOf[ScalaSettingsExtension],
      constructionArguments = settings
    )

    // `create` registers the extension on `settings`, which is what the settings script configures
    // (`scalaBackend { ... }`).
    // A project plugin cannot see that container: `Project` has no `Settings`.
    // `add` puts this same instance on `settings.getGradle` — Groovy `settings.gradle` —
    // the object a project plugin still has as `project.getGradle()`.
    // `findByType` searches only the container it is given, so without this `add` the project plugin
    // does not see the extension.
    // Each container has its own names. The same name in both is valid, and `findByType` does not use it.
    Extensions.add(
      settings.getGradle,
      ScalaSettingsExtension.gradleExtensionName,
      extension
    )
