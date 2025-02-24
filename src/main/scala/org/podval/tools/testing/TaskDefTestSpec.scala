package org.podval.tools.testing

import org.gradle.api.internal.tasks.testing.TestClassRunInfo
import org.podval.tools.testing.framework.FrameworkDescriptor
import org.podval.tools.testing.taskdef.TaskDefWriter
import sbt.testing.{Framework, Runner, TaskDef}

// This is used to convey the data about the test to the TestClassProcessor.
// For forked Scala tests, only the Framework name gets serialized and is instantiated as needed;
// *but* for ScalaJS Framework is retrieved from the Node side and can not be instantiated...
final class TaskDefTestSpec(
  val frameworkOrName: TaskDefTestSpec.FrameworkOrName,
  val taskDef: TaskDef
) extends TestClassRunInfo:
  override def getTestClassName: String = taskDef.fullyQualifiedName

  // Note: only used when the tests are forked, to transport TaskDefTestSpec to a TestWorker;
  // when not forked, the original instance is used.
  def write: String =
    val frameworkName: String = TaskDefTestSpec.frameworkName(frameworkOrName)
    s"$frameworkName#${TaskDefWriter.write(taskDef)}"

object TaskDefTestSpec:
  type FrameworkOrName = Either[String, Framework]

  def frameworkName(frameworkOrName: FrameworkOrName): String = frameworkOrName.fold(identity, _.name)

  def get(testClassRunInfo: TestClassRunInfo): TaskDefTestSpec = testClassRunInfo match
    case taskDefTestSpec: TaskDefTestSpec => taskDefTestSpec
    case _ =>
      val parts: Array[String] = testClassRunInfo.getTestClassName.split("#")
      TaskDefTestSpec(
        frameworkOrName = Left(parts.head),
        taskDef = TaskDefWriter.read(parts.tail.mkString("#"))
      )

  def makeRunner(
    frameworkOrName: FrameworkOrName,
    includeTags: Array[String],
    excludeTags: Array[String]
  ): Runner =
    val framework: Framework = frameworkOrName match
      case Right(framework) =>
        // not forking: just use the framework and its classloader
        framework
      case Left(frameworkName) =>
        // forking: instantiate
        FrameworkDescriptor(frameworkName)
          .newInstance
          .asInstanceOf[Framework]

    val args: Seq[String] = FrameworkDescriptor(framework.name).args(
      includeTags = includeTags,
      excludeTags = excludeTags
    )

    // Note: we are running the runner in *this* JVM, so remote arguments are not used?
    val remoteArgs: Seq[String] = Seq.empty

    val frameworkClassLoader: ClassLoader = framework.getClass.getClassLoader

    framework.runner(
      args.toArray,
      remoteArgs.toArray,
      frameworkClassLoader
    )
