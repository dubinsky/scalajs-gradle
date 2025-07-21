package org.podval.tools.test.run

import org.gradle.api.internal.tasks.testing.{TestClassProcessor, WorkerTestClassProcessorFactory}
import org.gradle.api.internal.tasks.testing.worker.WorkerTestClassProcessor
import org.gradle.internal.actor.ActorFactory
import org.gradle.internal.id.{CompositeIdGenerator, IdGenerator, LongIdGenerator}
import org.gradle.internal.time.Clock
import org.podval.tools.platform.Output

// This class gets serialized into the worker when tests are forked;
// thus it, its parameters, and everything reachable from it must be serializable,
// and thus it can not be an inner class of TestFramework,
// so no further simplifications seem possible.
final class RunTestClassProcessorFactory(
  includeTags: Array[String],
  excludeTags: Array[String],
  output: Output,
  dryRun: Boolean
) extends WorkerTestClassProcessorFactory with Serializable:

  private def create(
    idGenerator: IdGenerator[?],
    clock: Clock
  ): RunTestClassProcessor = RunTestClassProcessor(
    includeTags = includeTags,
    excludeTags = excludeTags,
    output = output,
    dryRun = dryRun,
    idGenerator = idGenerator,
    clock = clock
  )
  
  override def create(
    idGenerator: IdGenerator[?],
    actorFactory: ActorFactory,
    clock: Clock
  ): TestClassProcessor = ReadTestClassProcessor(
    create(
      idGenerator, 
      clock
    )
  )

  def createNonForking(
    clock: Clock
  ): TestClassProcessor =
    val workerId: AnyRef = Long.box(0)
    val idGenerator: IdGenerator[?] = CompositeIdGenerator(workerId, new LongIdGenerator)
    val processor: TestClassProcessor = create(
      idGenerator,
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
