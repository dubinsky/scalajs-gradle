package org.podval.tools.testing.processors

import org.gradle.api.internal.tasks.testing.{TestClassProcessor, WorkerTestClassProcessorFactory}
import org.gradle.api.internal.tasks.testing.worker.WorkerTestClassProcessor
import org.gradle.internal.id.{CompositeIdGenerator, IdGenerator, LongIdGenerator}
import org.gradle.internal.service.DefaultServiceRegistry
import org.gradle.internal.time.Clock

// ScalaJS tests must be run in the same JVM where they are discovered.
object NonForkingTestClassProcessor:

  // Note: all the started/completed events are reported, including those for the suite.
  def apply(
    workerTestClassProcessorFactory: WorkerTestClassProcessorFactory,
    clock: Clock
  ): TestClassProcessor =
    val workerId: AnyRef = Long.box(0)
    val idGenerator: IdGenerator[AnyRef] = CompositeIdGenerator(workerId, new LongIdGenerator)
    val processor:TestClassProcessor = workerTestClassProcessorFactory.create(
      // see org.gradle.api.internal.tasks.testing.worker.TestWorker.TestFrameworkServiceRegistry
      new DefaultServiceRegistry:
        protected def createClock: Clock = clock
        protected def createIdGenerator: IdGenerator[AnyRef] = idGenerator
    )
    
    val workerSuiteId: AnyRef = idGenerator.generateId()
    val workerDisplayName: String = s"Gradle Test Executor $workerId (non-forking)"
    WorkerTestClassProcessor(
      processor,
      workerSuiteId,
      workerDisplayName,
      clock
    )

