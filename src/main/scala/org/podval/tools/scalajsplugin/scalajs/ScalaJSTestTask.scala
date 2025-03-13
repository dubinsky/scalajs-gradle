package org.podval.tools.scalajsplugin.scalajs

import org.podval.tools.test.task.TestTask

abstract class ScalaJSTestTask extends TestTask with ScalaJSRunTask:
  final override protected def flavour: String = "Test"

  final override protected def linkTaskClass: Class[ScalaJSLinkTestTask] = classOf[ScalaJSLinkTestTask]

  // Note: Scala.js tests are not forkable; see org.scalajs.sbtplugin.ScalaJSPluginInternal
  final override protected def canFork: Boolean = false

  // TODO Note: Scala.js compiler does not seem to leave annotations in the classfile,
  // at least `TestClassVisitor.visitAnnotation()` does not get called...
  final override protected def canUseClassfileTestDetection: Boolean = false

  final override protected def testEnvironment: ScalaJSTestEnvironment = ScalaJSTestEnvironment(scalaJSRunCommon)
