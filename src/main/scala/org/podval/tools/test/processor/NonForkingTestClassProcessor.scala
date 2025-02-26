package org.podval.tools.test.processor

import org.gradle.api.internal.tasks.testing.{TestClassProcessor, WorkerTestClassProcessorFactory}
import org.gradle.api.internal.tasks.testing.worker.WorkerTestClassProcessor
import org.gradle.internal.actor.ActorFactory
import org.gradle.internal.id.{CompositeIdGenerator, IdGenerator, LongIdGenerator}
import org.gradle.internal.time.Clock

// ScalaJS tests must be run in the same JVM where they are discovered.
object NonForkingTestClassProcessor:

  // Note: all the started/completed events are reported, including those for the suite.
  def apply(
    workerTestClassProcessorFactory: WorkerTestClassProcessorFactory,
    actorFactory: ActorFactory,
    clock: Clock
  ): TestClassProcessor =
    val workerId: AnyRef = Long.box(0)
    val idGenerator: IdGenerator[?] = CompositeIdGenerator(workerId, new LongIdGenerator)
    val processor: TestClassProcessor = workerTestClassProcessorFactory.create(
      idGenerator,
      actorFactory,
      clock
    )
    
    val workerSuiteId: AnyRef = idGenerator.generateId()
    val workerDisplayName: String = s"Gradle Test Executor $workerId (non-forking)"
    WorkerTestClassProcessor(
      processor,
      workerSuiteId,
      workerDisplayName,
      clock
    )

