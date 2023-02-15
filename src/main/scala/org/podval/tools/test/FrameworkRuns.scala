package org.podval.tools.test

import org.podval.tools.test.framework.FrameworkDescriptor
import sbt.testing.{Framework, Runner}
import java.io.File
import java.net.URLClassLoader

final class FrameworkRuns(
  isForked: Boolean,
  testClassPath: Array[File],
  testTagsFilter: TestTagsFilter
):

  // TODO we should not need to carry testClassPath all the way here and can just use own classloader - mimonafshach:
  // - if we are running in Node, testClassLoader is ignored;
  // - if we are not forked, TestTaskScala.loadFrameworks() already added it to the classpath on which we are running.
  // - if we are forked, ForkingTestClassProcessor added it to the applicationClassPath on which we are running (?) - but no...
  private val testClassLoader: ClassLoader =
    if isForked
    then URLClassLoader(testClassPath.map(_.toURI.toURL))
    else getClass.getClassLoader

  import FrameworkRuns.Run

  private var frameworksRuns: Seq[Run] = Seq.empty
  def getRuns: Seq[Run] = frameworksRuns

  def getRunner(framework: Framework): Runner = synchronized {
    frameworksRuns.find(_.framework eq framework).map(_.runner).getOrElse {
      val frameworkDescriptor: FrameworkDescriptor = FrameworkDescriptor.forFramework(framework)

      val args: Array[String] = frameworkDescriptor.args(
        testTagsFilter = testTagsFilter
      )

      val runner: Runner = framework.runner(
        args,
        Array.empty,
        testClassLoader
      )

      val run: Run = Run(
        framework = framework,
        runner = runner
      )

      frameworksRuns = frameworksRuns :+ run

      runner
    }
  }

object FrameworkRuns:

  class Run(
    val framework: Framework,
    val runner: Runner
  )
