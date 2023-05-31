package org.podval.tools.scalajs

import org.gradle.api.Task
import org.opentorah.node.NodeExtension

trait ScalaJSTask extends Task:
  setDescription(s"$flavour ScalaJS")

  protected def flavour: String

  protected final def setUpNodeProject(): Unit = NodeExtension.get(getProject).setUpProject(List("jsdom"))
