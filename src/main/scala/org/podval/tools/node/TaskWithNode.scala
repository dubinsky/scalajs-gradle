package org.podval.tools.node

import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, InputDirectory}
import org.podval.tools.build.Gradle.*
import org.podval.tools.build.GradleBuildContextCore
import java.io.File

trait TaskWithNode extends Task:
  @Input def getVersion: Property[String]
  @InputDirectory def getGradleUserHomeDir: Property[File]
  @InputDirectory def getNodeModulesParent: Property[File]

  final def node: Node = NodeDependency
    .getInstalled(
      version = getVersion.toOption,
      context = GradleBuildContextCore(getGradleUserHomeDir.get, getLogger)
    )
    .node(
      nodeModulesParent = getNodeModulesParent.get
    )
