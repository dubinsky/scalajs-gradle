package org.podval.tools.scalajsplugin.scalajs

import org.podval.tools.test.task.TestTask

abstract class ScalaJSTestTask extends TestTask with ScalaJSRunTask:
  final override protected def flavour: String = "Test"

  final override protected def linkTaskClass: Class[ScalaJSLinkTestTask] = classOf[ScalaJSLinkTestTask]

  final override protected def isScalaJS: Boolean = true

  final override protected def createTestEnvironment: ScalaJSTestEnvironment = ScalaJSTestEnvironment(scalaJSRunCommon)
