package org.podval.tools.test.processor

import org.gradle.api.internal.tasks.testing.TestClassProcessor
import org.gradle.api.internal.tasks.testing.worker.WorkerTestClassProcessor
import org.gradle.api.logging.LogLevel
import org.gradle.internal.actor.ActorFactory
import org.gradle.internal.id.{CompositeIdGenerator, IdGenerator, LongIdGenerator}
import org.gradle.internal.time.Clock

// Note: this class gets serialized into the worker when tests are forked;
// thus it, its parameters, and everything reachable from it must be serializable,
// and thus it can not be an inner class of TestFramework,
// so no further simplifications seems feasible.
final class RunTestClassProcessorFactory(
  includeTags: Array[String],
  excludeTags: Array[String],
  runningInIntelliJIdea: Boolean,
  logLevelEnabled: LogLevel
) extends org.gradle.api.internal.tasks.testing.WorkerTestClassProcessorFactory with Serializable:

  private def create(
    idGenerator: IdGenerator[?],
    clock: Clock
  ): RunTestClassProcessor = RunTestClassProcessor(
    includeTags = includeTags,
    excludeTags = excludeTags,
    runningInIntelliJIdea = runningInIntelliJIdea,
    logLevelEnabled = logLevelEnabled,
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
