package org.podval.tools.test.taskdef

import org.podval.tools.test.framework.FrameworkDescriptor
import sbt.testing.{Framework, Runner}

abstract class FrameworkProvider:
  def frameworkName: String

  def framework: Framework
  
  final def frameworkDescriptor: FrameworkDescriptor = FrameworkDescriptor.forName(frameworkName)
  
  final def makeRunner(
    includeTags: Array[String],
    excludeTags: Array[String]
  ): Runner =
    val args: Array[String] = frameworkDescriptor.args(
      includeTags = includeTags,
      excludeTags = excludeTags
    )

    // We are running the runner in *this* JVM, so remote arguments are not used?
    val remoteArgs: Array[String] = Array.empty

    val frameworkClassLoader: ClassLoader = framework.getClass.getClassLoader

    framework.runner(
      args,
      remoteArgs,
      frameworkClassLoader
    )
    