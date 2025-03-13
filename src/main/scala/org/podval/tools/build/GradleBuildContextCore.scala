package org.podval.tools.build

import org.gradle.api.GradleException
import java.io.File

class GradleBuildContextCore(
  val gradleUserHomeDir: File
) extends BuildContextCore:
  final override def frameworks: File = gradleUserHomeDir
  
  final override def fatalError(message: String): Nothing = throw GradleException(s"Fatal error in $this: $message")
