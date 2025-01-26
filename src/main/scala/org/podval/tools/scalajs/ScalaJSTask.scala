package org.podval.tools.scalajs

import org.podval.tools.node.TaskWithNode

trait ScalaJSTask extends TaskWithNode:
  setDescription(s"$flavour ScalaJS")

  protected def flavour: String

  protected def linkTask: LinkTask
  
  protected final def setUpNodeProject(): Unit = setUpNodeProject(List("jsdom"))
  
  protected final def scalaJs: ScalaJS = ScalaJS(task = this, linkTask = linkTask)
