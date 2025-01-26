package org.podval.tools.build

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File

class GradleBuildContextCore(
  gradleUserHomeDir: File,
  override val logger: Logger
) extends BuildContextCore[Logger]:
  final override def frameworks: File = gradleUserHomeDir

  final override def lifecycle(message: String): Unit = logger.lifecycle(message)
  
  final override def fatalError(message: String): Nothing = throw GradleException(s"Fatal error in $this: $message")
