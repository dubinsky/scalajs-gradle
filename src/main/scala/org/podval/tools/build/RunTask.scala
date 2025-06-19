package org.podval.tools.build

import org.gradle.api.DefaultTask
import org.podval.tools.platform.OutputPiper
import org.podval.tools.test.task.TestTask

object RunTask:
  abstract class Test extends TestTask with BackendTask.Test

  abstract class Main extends DefaultTask with BackendTask.Main:
    final def outputPiper: OutputPiper = OutputPiper(
      out = getLogger.lifecycle,
      err = message => getLogger.error(s"err: $message")
    )
