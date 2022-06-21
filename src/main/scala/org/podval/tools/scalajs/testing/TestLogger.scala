package org.podval.tools.scalajs.testing

import org.gradle.api.logging.Logger
import org.gradle.api.tasks.testing.TestResult.ResultType
import sbt.testing.Logger as TLogger

final class TestLogger(val gradleLogger: Logger) extends TestsListener:
  override def doInit(): Unit = () // log.lifecycle(s"Testing: doInit()")

  override def doComplete(finalResult: ResultType): Unit = () // log.lifecycle(s"Testing: doComplete(${finalResult.toString})")

  override def startGroup(name: String): Unit = () // log.lifecycle(s"Testing: startGroup($name)")

  override def testEvent(event: TestEvent): Unit = () // log.lifecycle(s"Testing: testEvent(${event.toString})")

  override def endGroup(name: String, t: Throwable): Unit = () // log.lifecycle(s"Testing: endGroup($name, $t)")

  override def endGroup(name: String, result: ResultType): Unit = () // log.lifecycle(s"Testing: endGroup($name, $result)")

  override def contentLogger(@deprecated("unused", "") name: String): Option[ContentLogger] = Some(
    new ContentLogger:
      override val log: TLogger = new SBTTestingLogger(gradleLogger, name)
      override def flush(): Unit = ()
  )
