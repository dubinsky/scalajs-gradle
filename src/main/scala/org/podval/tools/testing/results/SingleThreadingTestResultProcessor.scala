package org.podval.tools.testing.results

import org.gradle.api.internal.tasks.testing.{TestCompleteEvent, TestDescriptorInternal, TestResultProcessor, TestStartEvent}
import org.gradle.api.tasks.testing.{TestFailure, TestOutputEvent}
import org.gradle.internal.actor.{Actor, ActorFactory}

// MaxNParallelTestClassProcessor used when forking wraps resultProcessor in an Actor already,
// bot when not - this class can be used to do it to ensure that all the calls come from the same thread.
// Note: So far, even without it things seem to work... strange!
class SingleThreadingTestResultProcessor(
  testResultProcessor: TestResultProcessor,
  actorFactory: ActorFactory
) extends TestResultProcessor:

  private val actor: Actor = actorFactory.createActor(testResultProcessor)

  private val delegate: TestResultProcessor = actor.getProxy(classOf[TestResultProcessor])

  private var rootId: Option[AnyRef] = None

  override def started(test: TestDescriptorInternal, event: TestStartEvent): Unit =
    if rootId.isEmpty then rootId = Some(test.getId)
    delegate.started(test, event)

  override def completed(testId: AnyRef, event: TestCompleteEvent): Unit =
    delegate.completed(testId, event)
    if rootId.contains(testId) then actor.stop()

  override def output(testId: AnyRef, event: TestOutputEvent): Unit =
    delegate.output(testId, event)

  override def failure(testId: AnyRef, failure: TestFailure): Unit =
    delegate.failure(testId, failure)
