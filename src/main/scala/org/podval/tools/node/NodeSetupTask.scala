package org.podval.tools.node

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.{Input, TaskAction}
import org.gradle.work.DisableCachingByDefault
import org.podval.tools.build.Version
import scala.jdk.CollectionConverters.ListHasAsScala

@DisableCachingByDefault(because = "Installs Node.js and npm modules.")
abstract class NodeSetupTask extends DefaultTask with NodeProjectTask:
  @Input def getModules: ListProperty[String]

  @TaskAction def install(): Unit = NodeInstaller
    .getInstalledOrInstall(
      version = Version(getVersion),
      project = getProject,
      output = output
    )
    .nodeProject(
      root = getNodeProjectRoot.get,
      runner = runner
    )
    .setUp(getModules.get.asScala.toList)
