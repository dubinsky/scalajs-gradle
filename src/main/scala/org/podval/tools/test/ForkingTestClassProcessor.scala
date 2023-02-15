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
import ForkingTestClassProcessor.Remote

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

      val remoteProcessor: RemoteTestClassProcessor = ForkingTestClassProcessor.createRemoteProcessor(
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
    val builder: WorkerProcessBuilder = workerFactory.create(TestWorker(processorFactory))
    builder.setBaseName("Gradle Test Executor")
    builder.setImplementationClasspath(
      (ForkingTestClassProcessor.getImplementationClasspath(moduleRegistry) ++ implementationClassPath).asJava
    )
    builder.setImplementationModulePath(
      ForkingTestClassProcessor.getUrls(implementationModules.map(moduleRegistry.getExternalModule)).asJava
    )
    builder.applicationClasspath(applicationClassPath.asJava)
    builder.applicationModulePath(applicationModulePath.asJava)
    options.copyTo(builder.getJavaCommand)
    builder.getJavaCommand.jvmArgs("-Dorg.gradle.native=false")
    builder.sharedPackages(sharedPackages.asJava)

    builder.build()

object ForkingTestClassProcessor:
  private final class Remote(
    val workerProcess: WorkerProcess,
    val remoteProcessor: RemoteTestClassProcessor,
    val completion: WorkerLeaseRegistry.WorkerLeaseCompletion
  )

  private def createRemoteProcessor(
    workerProcess: WorkerProcess,
    resultProcessor: TestResultProcessor
  ): RemoteTestClassProcessor =
    val connection: ObjectConnection = workerProcess.getConnection
    connection.useParameterSerializers(TestSerializerRegistry.create)
    connection.addIncoming(classOf[TestResultProcessor], resultProcessor)
    val remoteProcessor: RemoteTestClassProcessor = connection.addOutgoing(classOf[RemoteTestClassProcessor])
    connection.connect()
    remoteProcessor.startProcessing()
    remoteProcessor

  private def getUrls(modules: List[Module]): List[URL] = modules
    .flatMap(_.getImplementationClasspath.getAsURLs.asScala)

  private def getImplementationClasspath(moduleRegistry: ModuleRegistry): List[URL] = getUrls(
    List(
      "gradle-core-api",
      "gradle-core",
      "gradle-logging",
      "gradle-logging-api",
      "gradle-messaging",
      "gradle-base-services",
      "gradle-enterprise-logging",
      "gradle-enterprise-workers",
      "gradle-cli",
      "gradle-wrapper-shared",
      "gradle-native",
      "gradle-dependency-management",
      "gradle-workers",
      "gradle-worker-processes",
      "gradle-process-services",
      "gradle-persistent-cache",
      "gradle-model-core",
      "gradle-jvm-services",
      "gradle-files",
      "gradle-file-collections",
      "gradle-file-temp",
      "gradle-hashing",
      "gradle-snapshots",
      "gradle-base-annotations",
      "gradle-build-operations",

      "gradle-testing-base",
      "gradle-testing-jvm",
      "gradle-testing-junit-platform"
    ).map(moduleRegistry.getModule) ++
    List(
      "slf4j-api",
      "jul-to-slf4j",
      "native-platform",
      "kryo",
      "commons-lang",
      "guava",
      "javax.inject",
      // Note: test parallelization breaks without this starting with Gradle 7.6:
      //   java.lang.NoClassDefFoundError: org/codehaus/groovy/runtime/callsite/CallSite
      "groovy",
      "groovy-ant",
      "groovy-json",
      "groovy-xml",
      "asm",
      "javax.inject",
      "junit"
    ).map(moduleRegistry.getExternalModule)
  )

//"gradle-language-java"
//"gradle-language-jvm"
//"gradle-worker"
//"groovy-templates"
//"gradle-platform-base"


