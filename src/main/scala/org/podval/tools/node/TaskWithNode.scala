package org.podval.tools.node

import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, Internal}
import org.podval.tools.build.{Gradle, GradleBuildContextCore}
import java.io.File

trait TaskWithNode extends Task:
  @Input def getVersion: Property[String]
  @Internal def getGradleUserHomeDir: Property[File]
  @Internal def getNodeModulesParent: Property[File]

  final def node: Node = NodeDependency
    .getInstalled(
      version = Gradle.toOption(getVersion),
      context = GradleBuildContextCore(getGradleUserHomeDir.get)
    )
    .node(
      nodeModulesParent = getNodeModulesParent.get
    )
