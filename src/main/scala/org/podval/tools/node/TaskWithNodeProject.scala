package org.podval.tools.node

import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Internal, Optional}
import org.podval.tools.platform.{TaskWithGradleUserHomeDir, TaskWithRunner}
import java.io.File

trait TaskWithNodeProject extends TaskWithRunner with TaskWithGradleUserHomeDir:
  @Optional @Input def getVersion: Property[String]
  @Internal def getNodeProjectRoot: Property[File]

  final def nodeProject: NodeProject = NodeDependency
    .getInstalled(
      version = Option(getVersion.getOrNull),
      gradleUserHomeDir = getGradleUserHomeDir.get
    )
    .nodeProject(
      root = getNodeProjectRoot.get,
      runner = runner
    )
