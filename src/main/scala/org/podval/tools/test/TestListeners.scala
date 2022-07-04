package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.{DefaultTestOutputEvent, TestCompleteEvent, TestResultProcessor,
  TestStartEvent}
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.testing.{Test, TestOutputEvent, TestResult}
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.podval.tools.test.TestDescriptor
import scala.collection.mutable

// TODO rename TestReporter or TestListener
// TODO intercept the tests output: supply my own Pipes by copying a bit of the JSEnvRPC adapter code...
final class TestListeners(
  testResultProcessor: TestResultProcessor,
  sourceMapper: Option[SourceMapper],
  logger: Logger,
  useColours: Boolean
):
  import org.podval.tools.test.TestListeners.Event
  import org.podval.tools.test.TestListeners.Event.*

  def log(message: String): Unit = logger.lifecycle(message)

  // Note: Turns out that IntelliJ Idea integration only works when all the calls to
  // the IJ listener happen from the same thread (it uses some thread-local variable somewhere?).
  // Since some of the calls are caused by the call-back from the sbt testing interface's
  // event handler, I get "Test events were not received" in the Idea test UI.
  // It would have been nice if this fact was documented somewhere :(
  private val useDedicatedThread: Boolean = true

  private val queue: mutable.Queue[Event] = new mutable.Queue[Event]

  private val thread: Thread = new Thread:
    override def run(): Unit =
      while true do queue.synchronized(if queue.nonEmpty then send(queue.dequeue))

  if useDedicatedThread then thread.start()

  private def enqueue(event: Event): Unit =
    if useDedicatedThread
    then queue.synchronized(queue.enqueue(event))
    else send(event)

  private def send(event: Event): Unit = event match
    case Started(parentId, test, startTime) =>
      testResultProcessor.started(
        test,
        TestStartEvent(
          startTime,
          parentId
        )
      )

    case Completed(test, endTime) =>
      testResultProcessor.completed(
        test.getId,
        TestCompleteEvent(
          endTime
        )
      )

    case Failure(test, throwable) =>
      testResultProcessor.failure(
        test.getId,
        throwable
      )

    case Output(test, message, isError) =>
      testResultProcessor.output(
        test.getId,
        DefaultTestOutputEvent(
          if isError
          then TestOutputEvent.Destination.StdErr
          else TestOutputEvent.Destination.StdOut,
          message
        )
      )

  private def started(parentId: Object, test: TestDescriptor, startTime: Long): Unit =
    enqueue(Started(parentId, test, startTime))

  private def completed(test: TestDescriptor, endTime: Long): Unit =
    enqueue(Completed(test, endTime))

  def suiteStarted(parentId: Object, test: TestDescriptor.Suite): Unit =
    started(parentId, test, TestListeners.now)

  def suiteCompleted(test: TestDescriptor.Suite): Unit =
    completed(test, TestListeners.now)

  def methodStarted(parentId: Object, test: TestDescriptor.Method, startTime: Long): Unit =
    started(parentId, test, startTime)

  def methodFailed(test: TestDescriptor.Method, throwable: Throwable): Unit =
    enqueue(Failure(test, sourceMap(throwable)))

  def methodCompleted(test: TestDescriptor.Method, endTime: Long): Unit =
    completed(test, endTime)

  def error(
    test: TestDescriptor,
    throwable: Throwable
  ): Unit =
    logger.error(s"ERROR in test ${test.getId}", sourceMap(throwable))

  // Fired when during test execution anything is printed to standard output or error
  def output(test: TestDescriptor, message: String, isError: Boolean): Unit =
    enqueue(Output(test, message, isError))

  def contentLoggers(prefix: String): Array[sbt.testing.Logger] =
    def withPrefix(message: String): String = s"$prefix$message"
    Array(new sbt.testing.Logger:
      override def ansiCodesSupported: Boolean = useColours
      override def error(message: String): Unit = logger.error(withPrefix(message))
      override def warn (message: String): Unit = logger.warn (withPrefix(message))
      override def info (message: String): Unit = logger.info (withPrefix(message), null, null, null)
      override def debug(message: String): Unit = logger.debug(withPrefix(message), null, null, null)
      override def trace(t: Throwable   ): Unit = logger.error(withPrefix("ERROR"), t)
    )

  // Note: I think there is no equivalent for this in Gradle...
  final def flushContentLoggers(test: TestDescriptor): Unit = ()

  private def sourceMap(throwable: Throwable): Throwable =
    sourceMapper.fold(throwable)(_.sourceMap(throwable))

object TestListeners:
  sealed trait Event

  object Event:
    final case class Started(
      parentId: Object,
      test: TestDescriptor,
      startTime: Long
    ) extends Event

    final case class Completed(
      test: TestDescriptor,
      endTime: Long
    ) extends Event

    final case class Failure(
      test: TestDescriptor,
      throwable: Throwable
    ) extends Event

    final case class Output(
      test: TestDescriptor,
      message: String,
      isError: Boolean
    ) extends Event

  def now: Long = System.currentTimeMillis
