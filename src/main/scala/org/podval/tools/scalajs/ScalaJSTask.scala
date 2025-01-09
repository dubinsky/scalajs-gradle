package org.podval.tools.scalajs

import org.opentorah.node.TaskWithNode

trait ScalaJSTask extends TaskWithNode:
  setDescription(s"$flavour ScalaJS")

  protected def flavour: String

  protected final def setUpNodeProject(): Unit = setUpNodeProject(List("jsdom"))
