package org.podval.tools.scalanative

import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.podval.tools.build.RunTask
import org.podval.tools.nonjvm.TaskWithLink
import javax.inject.Inject
import scala.reflect.ClassTag

trait ScalaNativeRunTask[L <: ScalaNativeLinkTask : ClassTag] extends TaskWithLink[L]:
  final protected def run: ScalaNativeRun = ScalaNativeRun(
    binaryTestFile = linkTask.getOutputFile,
    logSource = getName
  )

object ScalaNativeRunTask:
  abstract class Main @Inject(execOperations: ExecOperations)
    extends RunTask.Main with ScalaNativeRunTask[ScalaNativeLinkTask.Main]:
    @TaskAction final def execute(): Unit = run.run(execOperations, outputPiper)

  abstract class Test extends RunTask.Test with ScalaNativeRunTask[ScalaNativeLinkTask.Test]:
    final override protected def createTestEnvironment: ScalaNativeBackend.TestEnvironment = run.createTestEnvironment
