package org.podval.tools.test.gradle

import org.gradle.api.Action
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.classpath.{Module, ModuleRegistry}
import org.gradle.api.internal.tasks.testing.{TestClassProcessor, TestClassRunInfo, TestResultProcessor,
  WorkerTestClassProcessorFactory}
import org.gradle.api.internal.tasks.testing.worker.{RemoteTestClassProcessor, TestEventSerializer, TestWorker}
import org.gradle.internal.remote.ObjectConnection
import org.gradle.internal.serialize.SerializerRegistry
import org.gradle.internal.work.{WorkerLeaseRegistry, WorkerThreadRegistry}
import org.gradle.process.internal.worker.{WorkerProcess, WorkerProcessBuilder, WorkerProcessContext, WorkerProcessFactory}
import org.gradle.process.JavaForkOptions
import org.gradle.process.internal.ExecException
import java.io.File
import java.net.URL
import java.util.concurrent.locks.{Lock, ReentrantLock}
import scala.jdk.CollectionConverters.*
import ForkingTestClassProcessor.Remote

// TODO Gradle PR: in org.gradle.api.internal.tasks.testing.worker.ForkingTestClassProcessor, introduce
//   createTestWorker
//   createParameterSerializers
class ForkingTestClassProcessor(
  workerThreadRegistry: WorkerThreadRegistry,
  workerFactory: WorkerProcessFactory,
  processorFactory: WorkerTestClassProcessorFactory,
  javaForkingOptions: JavaForkOptions,
  applicationClassPath: java.lang.Iterable[File],
  applicationModulePath: java.lang.Iterable[File],
  buildConfigAction: Action[WorkerProcessBuilder],
  moduleRegistry: ModuleRegistry,
  documentationRegistry: DocumentationRegistry
) extends TestClassProcessor:
  private val lock: Lock = ReentrantLock()

  private def withLock(body: => Unit): Unit =
    lock.lock()
    try
      body
    finally
      lock.unlock()

  private var resultProcessor: Option[TestResultProcessor] = None
  private var remote: Option[Remote] = None
  private var stoppedNow: Boolean = false

  override def startProcessing(resultProcessor: TestResultProcessor): Unit =
    this.resultProcessor = Some(resultProcessor)

  override def processTestClass(testClass: TestClassRunInfo): Unit = withLock(
    if !stoppedNow then
      if remote.isEmpty then remote = Some(createRemote)
      remote.get.remoteProcessor.processTestClass(testClass)
  )

  override def stop(): Unit =
    try
      for remote: Remote <- remote do
        withLock(if !stoppedNow then remote.remoteProcessor.stop())
        remote.workerProcess.waitForStop()
    catch case e: ExecException =>
      if !stoppedNow then throw ExecException(
        s"""${e.getMessage}
           |This problem might be caused by incorrect test process configuration.
           |Please refer to the test execution section in the User Manual at ${documentationRegistry.getDocumentationFor("java_testing", "sec:test_execution")}
           |""".stripMargin,
        e.getCause
      )
    finally
      for remote: Remote <- remote do remote.completion.leaseFinish()

  override def stopNow(): Unit = withLock {
    stoppedNow = true
    for remote: Remote <- remote do remote.workerProcess.stopNow()
  }

  private def createRemote: Remote =
    val completion: WorkerLeaseRegistry.WorkerLeaseCompletion = workerThreadRegistry.startWorker()
    try
      val workerProcess: WorkerProcess = createWorkerProcess
      workerProcess.start()

      val remoteProcessor: RemoteTestClassProcessor = createRemoteProcessor(
        workerProcess,
        resultProcessor.get
      )

      Remote(
        workerProcess,
        remoteProcessor,
        completion
      )
    catch case e: RuntimeException =>
      completion.leaseFinish()
      throw e

  private def createWorkerProcess: WorkerProcess =
    val builder: WorkerProcessBuilder = workerFactory.create(createTestWorker(processorFactory))
    builder.setBaseName("Gradle Test Executor")
    builder.setImplementationClasspath(ForkingTestClassProcessor.getTestWorkerImplementationClasspath(moduleRegistry).asJava)
    builder.applicationClasspath(applicationClassPath)
    builder.applicationModulePath(applicationModulePath)
    javaForkingOptions.copyTo(builder.getJavaCommand)
    builder.getJavaCommand.jvmArgs("-Dorg.gradle.native=false")
    buildConfigAction.execute(builder)

    builder.build()

  private def createRemoteProcessor(
    workerProcess: WorkerProcess,
    resultProcessor: TestResultProcessor
  ): RemoteTestClassProcessor =
    val connection: ObjectConnection = workerProcess.getConnection
    connection.useParameterSerializers(createParameterSerializers)
    connection.addIncoming(classOf[TestResultProcessor], resultProcessor)
    val remoteProcessor: RemoteTestClassProcessor = connection.addOutgoing(classOf[RemoteTestClassProcessor])
    connection.connect()
    remoteProcessor.startProcessing()
    remoteProcessor

  // TODO Gradle PR
  protected def createTestWorker(processorFactory: WorkerTestClassProcessorFactory): Action[WorkerProcessContext] =
    new TestWorker(processorFactory)

  // TODO Gradle PR
  protected def createParameterSerializers: SerializerRegistry = TestEventSerializer.create

object ForkingTestClassProcessor:
  private final class Remote(
    val workerProcess: WorkerProcess,
    val remoteProcessor: RemoteTestClassProcessor,
    val completion: WorkerLeaseRegistry.WorkerLeaseCompletion
  )

  private def getUrls(modules: List[Module]): List[URL] = modules
    .flatMap(_.getImplementationClasspath.getAsURLs.asScala)

  private def getTestWorkerImplementationClasspath(moduleRegistry: ModuleRegistry): List[URL] = getUrls(
    List(
      "gradle-core-api",
      "gradle-worker-processes",
      "gradle-core",
      "gradle-logging",
      "gradle-logging-api",
      "gradle-messaging",
      "gradle-files",
      "gradle-file-temp",
      "gradle-hashing",
      "gradle-base-services",
      "gradle-enterprise-logging",
      "gradle-enterprise-workers",
      "gradle-cli",
      "gradle-native",
      "gradle-testing-base",
      "gradle-testing-jvm-infrastructure",
      "gradle-testing-junit-platform",
      "gradle-process-services",
      "gradle-build-operations"
    ).map(moduleRegistry.getModule) ++
    List(
      "slf4j-api",
      "jul-to-slf4j",
      "native-platform",
      "kryo",
      "commons-lang",
      "javax.inject"
    ).map(moduleRegistry.getExternalModule)
  )
