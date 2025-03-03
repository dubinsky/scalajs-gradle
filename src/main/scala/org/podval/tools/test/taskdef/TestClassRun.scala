package org.podval.tools.test.taskdef

import org.gradle.api.internal.tasks.testing.TestClassRunInfo
import org.podval.tools.test.framework.FrameworkDescriptor
import sbt.testing.{Framework, Runner, TaskDef}

// This is used to convey the data about the test to the TestClassProcessor.
// For forked Scala tests, only the Framework name gets serialized and is instantiated as needed;
// *but* for Scala.js Framework is retrieved from the Node side and can not be instantiated,
// so the tests are run in the same JVM and framework itself is passed in.
abstract class TestClassRun(
  val taskDef: TaskDef
) extends TestClassRunInfo:

  final override def getTestClassName: String = taskDef.fullyQualifiedName

  final protected def frameworkDescriptor: FrameworkDescriptor = FrameworkDescriptor.forName(frameworkName)

  def frameworkName: String

  def framework: Framework

  final def makeRunner(
    includeTags: Array[String],
    excludeTags: Array[String]
  ): Runner =
    val args: Array[String] = frameworkDescriptor.args(
      includeTags = includeTags,
      excludeTags = excludeTags
    )
    
    // Note: we are running the runner in *this* JVM, so remote arguments are not used?
    val remoteArgs: Array[String] = Array.empty

    val frameworkClassLoader: ClassLoader = framework.getClass.getClassLoader

    framework.runner(
      args,
      remoteArgs,
      frameworkClassLoader
    )
