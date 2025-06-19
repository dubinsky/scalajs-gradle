package org.podval.tools.node

import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Internal, Optional}
import java.io.File

trait TaskWithNode extends Task:
  @Optional @Input def getVersion: Property[String]
  @Internal def getGradleUserHomeDir: Property[File]
  @Internal def getNodeModulesParent: Property[File]

  final def node: Node = NodeDependency
    .getInstalled(
      version = Option(getVersion.getOrNull),
      gradleUserHomeDir = getGradleUserHomeDir.get
    )
    .node(
      nodeModulesParent = getNodeModulesParent.get
    )
