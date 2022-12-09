package org.podval.tools.test

import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.classpath.{Module, ModuleRegistry}
import org.gradle.api.internal.tasks.testing.{TestResultProcessor, TestClassRunInfo}
import org.gradle.api.internal.tasks.testing.worker.RemoteTestClassProcessor
import org.gradle.internal.remote.ObjectConnection
import org.gradle.internal.work.{WorkerLeaseRegistry, WorkerThreadRegistry}
import org.gradle.process.internal.worker.{WorkerProcess, WorkerProcessBuilder, WorkerProcessFactory}
import org.gradle.process.JavaForkOptions
import org.gradle.process.internal.ExecException
import org.opentorah.build.Gradle
import java.io.File
import java.net.URL
import java.util.concurrent.locks.{Lock, ReentrantLock}
import scala.jdk.CollectionConverters.*

// Note: translated from org.gradle.api.internal.tasks.testing.worker.ForkingTestClassProcessor and modified to:
// - use my TestSerializerRegistry instead of org.gradle.api.internal.tasks.testing.worker.TestEventSerializer;
// - use my TestWorker instead of org.gradle.api.internal.tasks.testing.worker.TestWorker
//   (so that it can substitute my TestSerializerRegistry on the other end)
// - use Scala types for parameters and internally;
// - clean up parameter names;
// - make configuration process more straightforward (no Actions, just parameters).
final class ForkingTestClassProcessor(
  workerThreadRegistry: WorkerThreadRegistry,
  workerFactory: WorkerProcessFactory,
  processorFactory: WorkerTestClassProcessorFactory,
  options: JavaForkOptions,
  applicationClassPath: Iterable[File],
  applicationModulePath: Iterable[File],
  implementationClassPath: List[URL],
  implementationModules: List[String],
  sharedPackages: List[String],
  moduleRegistry: ModuleRegistry,
  documentationRegistry: DocumentationRegistry
) extends org.gradle.api.internal.tasks.testing.TestClassProcessor:
  private val lock: Lock = ReentrantLock()

  private var resultProcessor: TestResultProcessor = _
  private var remoteProcessor: RemoteTestClassProcessor = _
  private var workerProcess: WorkerProcess = _
  private var completion: WorkerLeaseRegistry.WorkerLeaseCompletion = _
  private var stoppedNow: Boolean = _

  override def startProcessing(resultProcessor: TestResultProcessor): Unit =
    this.resultProcessor = resultProcessor

  override def processTestClass(testClass: TestClassRunInfo): Unit =
    lock.lock()
    try
      if stoppedNow then return

      if remoteProcessor == null then
        completion = workerThreadRegistry.startWorker()

        try
          this.remoteProcessor = forkProcess
        catch case e: RuntimeException =>
          completion.leaseFinish()
          completion = null
          throw e

      remoteProcessor.processTestClass(testClass)
    finally
      lock.unlock()

  override def stop(): Unit =
    try
      if remoteProcessor != null then
        lock.lock()
        try
          if !stoppedNow then remoteProcessor.stop()
        finally
          lock.unlock()
        workerProcess.waitForStop()
    catch case e: ExecException =>
      if !stoppedNow then throw ExecException(
        s"""${e.getMessage}
           |This problem might be caused by incorrect test process configuration.
           |Please refer to the test execution section in the User Manual at ${documentationRegistry.getDocumentationFor("java_testing", "sec:test_execution")}
           |""".stripMargin,
        e.getCause
      )
    finally
      if completion != null then completion.leaseFinish()

  override def stopNow(): Unit =
    lock.lock()
    try
      stoppedNow = true
      if remoteProcessor != null then workerProcess.stopNow()
    finally
      lock.unlock()

  private def forkProcess: RemoteTestClassProcessor =
    val builder: WorkerProcessBuilder = workerFactory.create(TestWorker(processorFactory))
    builder.setBaseName("Gradle Test Executor")
    builder.setImplementationClasspath((getImplementationClasspath ++ implementationClassPath).asJava)
    builder.setImplementationModulePath(
      ForkingTestClassProcessor.getUrls(implementationModules.map(moduleRegistry.getExternalModule)).asJava
    )
    builder.applicationClasspath(applicationClassPath.asJava)
    builder.applicationModulePath(applicationModulePath.asJava)
    options.copyTo(builder.getJavaCommand)
    builder.getJavaCommand.jvmArgs("-Dorg.gradle.native=false")
    builder.sharedPackages(sharedPackages.asJava)

    workerProcess = builder.build()
    workerProcess.start()
    val connection: ObjectConnection = workerProcess.getConnection
    connection.useParameterSerializers(TestSerializerRegistry.create)
    connection.addIncoming(classOf[TestResultProcessor], resultProcessor)
    val remoteProcessor: RemoteTestClassProcessor = connection.addOutgoing(classOf[RemoteTestClassProcessor])
    connection.connect()
    remoteProcessor.startProcessing()
    remoteProcessor

  private def getImplementationClasspath: List[URL] = ForkingTestClassProcessor.getUrls(
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
      "gradle-testing-jvm",
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
      "junit",
      "javax.inject",
      // Note: test parallelization breaks without this starting with Gradle 7.6:
      // java.lang.NoClassDefFoundError: org/codehaus/groovy/runtime/callsite/CallSite;
      // with it, the tests run but never terminate if parallelized...
      "groovy"
    ).map(moduleRegistry.getExternalModule)
  )

object ForkingTestClassProcessor:
  private def getUrls(modules: List[Module]): List[URL] = modules
    .flatMap(_.getImplementationClasspath.getAsURLs.asScala)
