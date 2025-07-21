package org.podval.tools.scalajs

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.podval.tools.node.TaskWithNodeProject
import org.podval.tools.nonjvm.RunTask
import scala.reflect.ClassTag

trait ScalaJSRunTask[L <: ScalaJSLinkTask : ClassTag] extends RunTask[ScalaJSBackend.type, L] with TaskWithNodeProject:
  @Input def getJsEnv: Property[String]
  JSEnvKind.convention(getJsEnv)

  @Input def getBrowserName: Property[String]
  BrowserName.convention(getBrowserName)

  final override protected def run: ScalaJSRun = ScalaJSRun(
    jsEnvKind = JSEnvKind(getJsEnv),
    nodeProject = nodeProject,
    browserName = BrowserName(getBrowserName),
    link = linkTask.link,
    output = output
  )

object ScalaJSRunTask:
  abstract class Main extends RunTask.Main[ScalaJSBackend.type, ScalaJSLinkTask.Main] with ScalaJSRunTask[ScalaJSLinkTask.Main]
  abstract class Test extends RunTask.Test[ScalaJSBackend.type, ScalaJSLinkTask.Test] with ScalaJSRunTask[ScalaJSLinkTask.Test]
