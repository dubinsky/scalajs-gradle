package org.podval.tools.scalajs

import org.podval.tools.test.{TestEnvironment, TestTask}

class Test extends TestTask with AfterLink:
  final override protected def flavour: String = "Test"
  final override protected def linkTaskClass: Class[Link.Test] = classOf[Link.Test]
  final override protected def testEnvironment: TestEnvironment = act(_.testEnvironment)
