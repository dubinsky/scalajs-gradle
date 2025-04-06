package org.podval.tools.test.run

import org.podval.tools.test.taskdef.FrameworkProvider
import org.podval.tools.util.Scala212Collections.{arrayAppend, arrayFind, arrayForEach}
import sbt.testing.Runner

final class Runners:
  private var runners: Array[(String, Runner)] = Array.empty

  def get(
    frameworkProvider: FrameworkProvider,
    includeTags: Array[String],
    excludeTags: Array[String]
  ): Runner = synchronized:
    val frameworkName: String = frameworkProvider.frameworkName
    arrayFind(runners, _._1 == frameworkName).map(_._2).getOrElse:
      val runner: Runner = frameworkProvider.makeRunner(
        includeTags,
        excludeTags
      )
      runners = arrayAppend(runners, (frameworkName, runner))
      runner

  def stop(done: (String, String) => Unit): Unit =
    arrayForEach(runners, (frameworkName, runner) => done(frameworkName, runner.done))
    