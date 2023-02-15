package org.podval.tools.test

import org.gradle.api.Action
import org.gradle.api.internal.tasks.testing.{TestClassProcessor, TestClassRunInfo, TestResultProcessor,
  WorkerTestClassProcessorFactory}
import org.gradle.api.internal.tasks.testing.worker.RemoteTestClassProcessor
import org.gradle.internal.UncheckedException
import org.gradle.internal.actor.ActorFactory
import org.gradle.internal.actor.internal.DefaultActorFactory
import org.gradle.internal.concurrent.{DefaultExecutorFactory, ExecutorFactory, Stoppable}
import org.gradle.internal.dispatch.ContextClassLoaderProxy
import org.gradle.internal.id.{CompositeIdGenerator, IdGenerator, LongIdGenerator}
import org.gradle.internal.remote.ObjectConnection
import org.gradle.internal.service.{DefaultServiceRegistry, ServiceRegistry}
import org.gradle.internal.time.Clock
import org.gradle.process.internal.worker.WorkerProcessContext
import org.slf4j.{Logger, LoggerFactory}
import java.io.Serializable
import java.security.AccessControlException
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

// Note: translated from org.gradle.api.internal.tasks.testing.worker.TestWorker and modified:
// - to use my TestSerializerRegistry instead of org.gradle.api.internal.tasks.testing.worker.TestEventSerializer;
// - to use my WorkerTestClassProcessor instead of org.gradle.api.internal.tasks.testing.worker.WorkerTestClassProcessor;
final class TestWorker(
  factory: WorkerTestClassProcessorFactory
) extends Action[WorkerProcessContext]
  with RemoteTestClassProcessor
  with Serializable
  with Stoppable:

  import TestWorker.State

  private given CanEqual[SecurityManager, SecurityManager] = CanEqual.derived

  private val runQueue: BlockingQueue[Runnable] = new ArrayBlockingQueue[Runnable](1)

  private var processor: TestClassProcessor = _
  private var resultProcessor: TestResultProcessor = _

  @volatile private var state: State = State.INITIALIZING

  override def execute(workerProcessContext: WorkerProcessContext): Unit =
    TestWorker.setThreadName()
    TestWorker.logger.info(s"${workerProcessContext.getDisplayName} started executing tests.")

    val securityManager: SecurityManager  = System.getSecurityManager
    System.setProperty(TestWorker.WORKER_ID_SYS_PROPERTY, workerProcessContext.getWorkerId.toString)

    val testServices: DefaultServiceRegistry = TestWorker.TestFrameworkServiceRegistry(workerProcessContext)
    startReceivingTests(workerProcessContext, testServices)

    try
      try
        while state != State.STOPPED do
          try
            runQueue.take().run()
          finally
            // Reset the thread name if the action changes it (e.g. if a test sets the thread name without resetting it afterwards)
            TestWorker.setThreadName()
      catch case e: InterruptedException => throw UncheckedException.throwAsUncheckedException(e)
    finally
      TestWorker.logger.info(s"${workerProcessContext.getDisplayName} finished executing tests.")

      synchronized {
        state = State.STOPPED
        runQueue.clear()
      }

      if System.getSecurityManager != securityManager then
        try System.setSecurityManager(securityManager)
        catch case e: SecurityException => TestWorker.logger.warn("Unable to reset SecurityManager. Continuing anyway...", e)

      testServices.close()

  private def startReceivingTests(
    workerProcessContext: WorkerProcessContext,
    testServices: ServiceRegistry
  ): Unit =
    val targetProcessor: TestClassProcessor = WorkerTestClassProcessor(factory.create(testServices))

    val proxy: ContextClassLoaderProxy[TestClassProcessor] = ContextClassLoaderProxy[TestClassProcessor](
      classOf[TestClassProcessor],
      targetProcessor,
      workerProcessContext.getApplicationClassLoader
    )

    processor = proxy.getSource

    val serverConnection: ObjectConnection = workerProcessContext.getServerConnection

    serverConnection.useParameterSerializers(TestSerializerRegistry.create)

    resultProcessor = serverConnection.addOutgoing(classOf[TestResultProcessor])
    serverConnection.addIncoming(classOf[RemoteTestClassProcessor], this)
    serverConnection.connect()

  override def startProcessing(): Unit = submitToRun(() =>
    if state != State.INITIALIZING
    then throw IllegalStateException("A command to start processing has already been received")
    processor.startProcessing(resultProcessor)
    state = State.STARTED
  )

  override def processTestClass(testClass: TestClassRunInfo): Unit = submitToRun(() =>
    if state != State.STARTED
    then throw IllegalStateException("Test classes cannot be processed until a command to start processing has been received")
    try
      processor.processTestClass(testClass)
    catch case e: AccessControlException => throw e
    finally
      // Clean the interrupted status
      Thread.interrupted()
  )

  override def stop(): Unit = submitToRun(() =>
    try
      processor.stop()
    finally
      state = State.STOPPED
      // Clean the interrupted status
      // because some test class processors do work here, e.g. JUnitPlatform
      Thread.interrupted()
  )

  private def submitToRun(command: Runnable): Unit = synchronized {
    if state != State.STOPPED then
      try runQueue.put(command)
      catch case e: InterruptedException => throw UncheckedException.throwAsUncheckedException(e)
  }

object TestWorker:
  private enum State derives CanEqual:
    case INITIALIZING, STARTED, STOPPED

  private val logger: Logger = LoggerFactory.getLogger(classOf[TestWorker])

  private val WORKER_ID_SYS_PROPERTY: String = "org.gradle.test.worker"
  val WORKER_TMPDIR_SYS_PROPERTY: String = "org.gradle.internal.worker.tmpdir" // TODO not used in the original either?
  private val WORK_THREAD_NAME: String = "Test worker"

  private def setThreadName(): Unit = Thread.currentThread.setName(TestWorker.WORK_THREAD_NAME)

  private class TestFrameworkServiceRegistry(workerProcessContext: WorkerProcessContext) extends DefaultServiceRegistry:
    protected def createClock: Clock =
      workerProcessContext.getServiceRegistry.get(classOf[Clock])

//    protected def createIdGenerator: IdGenerator[Object] =
//      CompositeIdGenerator(
//        workerProcessContext.getWorkerId,
//        new LongIdGenerator
//      )

    protected def createExecutorFactory: ExecutorFactory =
      new DefaultExecutorFactory

    protected def createActorFactory(executorFactory: ExecutorFactory): ActorFactory =
      new DefaultActorFactory(executorFactory)
