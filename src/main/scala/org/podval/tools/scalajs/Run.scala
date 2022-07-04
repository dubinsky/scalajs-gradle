package org.podval.tools.scalajs

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class Run extends DefaultTask with AfterLink:
  setGroup("other")
  
  final override protected def flavour: String = "Run"
  
  final override protected def linkTaskClass: Class[Link.Main] = classOf[Link.Main]
  
  @TaskAction final def execute(): Unit = act(_.run())
