package org.podval.tools.scalanative

import org.gradle.api.tasks.CacheableTask
import org.podval.tools.nonjvm.RunTask
import scala.reflect.ClassTag

trait ScalaNativeRunTask[L <: ScalaNativeLinkTask : ClassTag] extends RunTask[ScalaNativeBackend.type, L]:
  final override protected def run: ScalaNativeRun = ScalaNativeRun(
    binaryTestFile = linkTask.getOutputFile,
    output = output
  )

object ScalaNativeRunTask:
  @CacheableTask
  abstract class Main extends RunTask.Main[ScalaNativeBackend.type, ScalaNativeLinkTask.Main] with ScalaNativeRunTask[ScalaNativeLinkTask.Main]

  @CacheableTask
  abstract class Test extends RunTask.Test[ScalaNativeBackend.type, ScalaNativeLinkTask.Test] with ScalaNativeRunTask[ScalaNativeLinkTask.Test]
