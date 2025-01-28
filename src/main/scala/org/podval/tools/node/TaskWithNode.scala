package org.podval.tools.node

import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, OutputDirectory}
import org.podval.tools.build.GradleBuildContextCore
import java.io.File

trait TaskWithNode extends Task:
  @Input def getVersion: Property[String]
  @OutputDirectory def getGradleUserHomeDir: Property[File]
  @OutputDirectory def getNodeModulesParent: Property[File]

  final def node: Node = NodeDependency
    .getInstalled(
      version = NodeExtension.toOption(getVersion),
      context = GradleBuildContextCore(getGradleUserHomeDir.get, getLogger)
    )
    .node(
      nodeModulesParent = getNodeModulesParent.get
    )
