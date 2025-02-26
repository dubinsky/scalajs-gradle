package org.podval.tools.test.task

import org.gradle.api.logging.LogLevel
import org.gradle.internal.actor.ActorFactory
import org.gradle.internal.id.IdGenerator
import org.gradle.internal.time.Clock
import org.podval.tools.test.processor.WorkerTestClassProcessor

// Note: this class gets serialized into the worker (and is the only such class);
// thus it, its parameters and everything reachable from it must be serializable,
// and thus it can not be an inner class of TestFramework,
// so no further simplifications seems feasible.
final class WorkerTestClassProcessorFactory(
  includeTags: Array[String],
  excludeTags: Array[String],
  runningInIntelliJIdea: Boolean,
  logLevelEnabled: LogLevel
) extends org.gradle.api.internal.tasks.testing.WorkerTestClassProcessorFactory with Serializable:
  override def create(
    idGenerator: IdGenerator[?],
    actorFactory: ActorFactory,
    clock: Clock
  ): WorkerTestClassProcessor = WorkerTestClassProcessor(
    // Converting arrays to sets here with `.toSet` results in
    // java.lang.NoSuchMethodError: 'scala.collection.mutable.ArraySeq$ofRef scala.Predef$.wrapRefArray(java.lang.Object[])'
    // on Scala 2.12.
    includeTags = includeTags,
    excludeTags = excludeTags,
    runningInIntelliJIdea = runningInIntelliJIdea,
    logLevelEnabled = logLevelEnabled,
    idGenerator = idGenerator,
    clock = clock
  )
