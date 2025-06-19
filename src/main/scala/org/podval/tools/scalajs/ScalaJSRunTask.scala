package org.podval.tools.scalajs

import org.gradle.api.provider.Property
import org.gradle.api.tasks.{Input, TaskAction}
import org.podval.tools.build.RunTask
import org.podval.tools.node.TaskWithNode
import org.podval.tools.nonjvm.TaskWithLink
import scala.reflect.ClassTag

trait ScalaJSRunTask[L <: ScalaJSLinkTask : ClassTag] extends TaskWithLink[L] with TaskWithNode:
  @Input def getJsEnv: Property[String]
  JSEnvKind.convention(getJsEnv)

  @Input def getBrowserName: Property[String]
  BrowserName.convention(getBrowserName)

  final protected def run: ScalaJSRun = ScalaJSRun(
    jsEnvKind = JSEnvKind(getJsEnv),
    node = node,
    browserName = BrowserName(getBrowserName),
    link = linkTask.link,
    logSource = getName
  )

object ScalaJSRunTask:
  abstract class Main extends RunTask.Main with ScalaJSRunTask[ScalaJSLinkTask.Main]:
    @TaskAction final def execute(): Unit = run.run(outputPiper)

  abstract class Test extends RunTask.Test with ScalaJSRunTask[ScalaJSLinkTask.Test]:
    final override protected def createTestEnvironment: ScalaJSBackend.TestEnvironment = run.createTestEnvironment
