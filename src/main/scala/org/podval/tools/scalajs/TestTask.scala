package org.podval.tools.scalajs

import org.gradle.api.tasks.TaskAction
import org.podval.tools.scalajs.testing.{Listeners, TestLogger, Tests}

abstract class TestTask[T <: LinkTask.Test](clazz: Class[T]) extends AfterLinkTask[T](clazz):
  final override protected def flavour: String = "Test"
  
  @TaskAction final def execute(): Unit = Tests.run(
    jsEnv,
    input, // TODO is running tests really conditional on the existence of the 'main' module?
    analysis = linkTask.scalaCompileAnalysis,
    jsLogger = jsLogger,
    listeners = Listeners(
      listeners = Seq(TestLogger(getLogger)),
      log = getLogger
    )
  )

object TestTask:
  class FastOpt   extends TestTask(classOf[LinkTask.Test.FastOpt  ]) with ScalaJSTask.FastOpt
  class FullOpt   extends TestTask(classOf[LinkTask.Test.FullOpt  ]) with ScalaJSTask.FullOpt
  class Extension extends TestTask(classOf[LinkTask.Test.Extension]) with ScalaJSTask.Extension
