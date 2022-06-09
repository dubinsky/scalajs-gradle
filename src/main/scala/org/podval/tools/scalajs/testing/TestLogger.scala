package org.podval.tools.scalajs.testing

import org.gradle.api.logging.Logger

final class TestLogger(val log: Logger) extends TestsListener:
  override def doInit(): Unit = () // log.lifecycle(s"Testing: doInit()")

  override def doComplete(finalResult: TestResult): Unit = () // log.lifecycle(s"Testing: doComplete(${finalResult.toString})")

  override def startGroup(name: String): Unit = () // log.lifecycle(s"Testing: startGroup($name)")

  override def testEvent(event: TestEvent): Unit = () // log.lifecycle(s"Testing: testEvent(${event.toString})")

  override def endGroup(name: String, t: Throwable): Unit = () // log.lifecycle(s"Testing: endGroup($name, $t)")

  override def endGroup(name: String, result: TestResult): Unit = () // log.lifecycle(s"Testing: endGroup($name, $result)")

  override def contentLogger(@deprecated("unused", "") test: TestDefinition): Option[ContentLogger] =
    Some(new ContentLogger(new SBTTestingLogger(log), () => ()))
