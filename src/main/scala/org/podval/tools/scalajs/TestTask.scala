package org.podval.tools.scalajs

import org.gradle.api.tasks.TaskAction
import org.podval.tools.scalajs.testing.{TestLogger, Tests}
import sbt.testing.Framework

abstract class TestTask[T <: LinkTask.Test](clazz: Class[T]) extends AfterLinkTask[T](clazz):
  setDescription(s"Run ScalaJS${stage.description}")
  setGroup("build")

  @TaskAction final def execute(): Unit = Tests.run(
    jsEnv,
    input,
    analysis = linkTask.scalaCompileAnalysis,
    jsLogger = jsLogger,
    logger = TestLogger(getLogger),
    log = getLogger
  )

object TestTask:
  class FastOpt   extends TestTask(classOf[LinkTask.Test.FastOpt  ]) with ScalaJSTask.FastOpt
  class FullOpt   extends TestTask(classOf[LinkTask.Test.FullOpt  ]) with ScalaJSTask.FullOpt
  class Extension extends TestTask(classOf[LinkTask.Test.Extension]) with ScalaJSTask.Extension
