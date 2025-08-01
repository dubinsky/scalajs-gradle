package org.podval.tools.test.task

import org.gradle.api.Action
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.TestFramework
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.TestFilter as TestFilterG
import org.gradle.internal.Factory
import org.gradle.process.internal.worker.{DefaultWorkerProcessBuilder, WorkerProcessBuilder}
import org.podval.tools.build.TestEnvironment
import org.podval.tools.gradle.GradleClasspath
import org.podval.tools.platform.Output
import org.podval.tools.test.detect.SbtTestFrameworkDetector
import org.podval.tools.test.filter.TestFilter
import org.podval.tools.test.run.RunTestClassProcessorFactory
import org.podval.tools.util.Files
import java.io.File
import java.lang.reflect.Field
import java.net.URL
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava, SetHasAsScala}

class SbtTestFramework(
  defaultTestFilter: DefaultTestFilter,
  options: SbtTestFrameworkOptions,
  moduleRegistry: ModuleRegistry,
  testTaskTemporaryDir: Factory[File],
  dryRun: Provider[java.lang.Boolean],
  // delayed
  output: () => Output,
  testEnvironment: () => TestEnvironment[?]
) extends TestFramework:

  override def getOptions: SbtTestFrameworkOptions = options

  override def copyWithFilters(newTestFilters: TestFilterG): SbtTestFramework =
    val copiedOptions: SbtTestFrameworkOptions = SbtTestFrameworkOptions()
    copiedOptions.copyFrom(this.options)
    SbtTestFramework(
      newTestFilters.asInstanceOf[DefaultTestFilter],
      copiedOptions,
      this.moduleRegistry,
      this.testTaskTemporaryDir,
      this.dryRun,
      this.output,
      this.testEnvironment
    )
  
  private var detector: Option[SbtTestFrameworkDetector] = None

  override def getDetector: SbtTestFrameworkDetector =
    if detector.isEmpty then detector = Some(createDetector)
    detector.get

  override def close(): Unit = detector = None

  private def createDetector: SbtTestFrameworkDetector =
    val out: Output = output()
    out.info("SbtTestFramework", s"--tests ${defaultTestFilter.getCommandLineIncludePatterns}; build file includes: ${defaultTestFilter.getIncludePatterns}.")

    SbtTestFrameworkDetector(
      output = out,
      testEnvironment = testEnvironment(),
      testTaskTemporaryDir = testTaskTemporaryDir,
      testFilter = TestFilter(
        includes = defaultTestFilter.getIncludePatterns.asScala.toSet,
        excludes = defaultTestFilter.getExcludePatterns.asScala.toSet,
        commandLineIncludes = defaultTestFilter.getCommandLineIncludePatterns.asScala.toSet
      )
    )

  override def getProcessorFactory: RunTestClassProcessorFactory = RunTestClassProcessorFactory(
    includeTags = options.getIncludeCategories.asScala.toArray,
    excludeTags = options.getExcludeCategories.asScala.toArray,
    output = output(),
    dryRun = dryRun.get
  )

  // I need to make sure that the plugin classes themselves are on the worker's classpath(s).
  // If I add the jar to the *application* classpath, I start getting ClassNotFoundException and have to
  // add many Gradle modules to the application classpath and share "org.gradle" with the implementation classpath -
  // and I still get ClassNotFoundExceptions.
  //
  // If I add the plugin jar to the *implementation* classpath everything works (but feels unclean).
  private def classPathAdditions(
    gradleModules: List[String],
    externalModules: List[String],
    jars: List[String]
  ): List[URL] =
    jars.map(GradleClasspath.findOn) ++
    (
      gradleModules.map(moduleRegistry.getModule) ++
      externalModules.map(moduleRegistry.getExternalModule)
    ).flatMap(_.getImplementationClasspath.getAsURLs.asScala)

  private val implementationClasspathAdditions: List[URL] = classPathAdditions(
    gradleModules = List(),
    // To have the freedom to place the code where I want without trying to segregate forkable code from un-forkable
    // and avoid initialization issues, I need to add some Gradle modules.
    externalModules = List(
      //"gradle-core-api"
    ),
    jars = List(
      "org.podval.tools.scalajs",

      // Without this, when running framework tests on Scala 2.13 I get (on JVM only):
      // java.lang.NoClassDefFoundError: scala/runtime/LazyVals$
      //	at org.podval.tools.test.framework.FrameworkDescriptor.<clinit>(FrameworkDescriptor.scala:36)
      //	at org.podval.tools.test.framework.FrameworkDescriptor$.<clinit>(FrameworkDescriptor.scala:37)
      //	at org.podval.tools.test.TaskDefTestSpec$.makeRunner(TaskDefTestSpec.scala:48)
      //	at org.podval.tools.test.processor.WorkerTestClassProcessor.getRunner$$anonfun$1(WorkerTestClassProcessor.scala:115)
      // when trying to look up FrameworkDescriptor by name when running forked;
      // it does not seem to break anything even on Scala.js and even on Scala 2.12.
      "scala3-library_3"
    )
  )

  private val applicationClasspathAdditions: List[URL] = classPathAdditions(
    gradleModules = List(),
    externalModules = List(
      // Without this, starting with Gradle 7.6 I get:
      // java.lang.NoClassDefFoundError: org/codehaus/groovy/runtime/callsite/CallSite
      "groovy"
    ),
    jars = List()
  )

  private def sharedPackages: List[String] =
    // Since DefaultTestExecuter calls TestFramework.getWorkerConfigurationAction
    // before calling TestFramework.getDetector and setting its classpath,
    // and this is used in the getWorkerConfigurationAction,
    // we do not yet know what frameworks were actually loaded,
    // so we have to take into account all that could load - depending on the back-end in use.
    testEnvironment().frameworks.flatMap(_.sharedPackages) ++
    List(
      // Scala 3 and Scala 2 libraries;
      // when running on Scala 3, both jars themselves are already on the classpath;
      // when running on Scala 2, Scala 2 library jar is already on the classpath,
      // and Scala 3 library jar is added to the implementation classpath above.
      "scala",

      // "test-interface"; jar itself is already on the classpath
      "sbt.testing",

      // "groovy" external module added to the applicationClasspath
      "org.codehaus.groovy"
    )

  override def getWorkerConfigurationAction: Action[WorkerProcessBuilder] = (builderInterface: WorkerProcessBuilder) =>
    val builder: DefaultWorkerProcessBuilder = builderInterface.asInstanceOf[DefaultWorkerProcessBuilder]

    builder.setImplementationClasspath((
      SbtTestFramework.getImplementationClasspath(builder).asScala.toList ++
      implementationClasspathAdditions
    ).asJava)

    builder.applicationClasspath(
      applicationClasspathAdditions.map(Files.url2file).asJava
    )

    builder.sharedPackages(sharedPackages.asJava)

    ()

object SbtTestFramework:
  private def getImplementationClasspath(builder: DefaultWorkerProcessBuilder): java.util.List[URL] =
    val implementationClasspath: Field = classOf[DefaultWorkerProcessBuilder].getDeclaredField("implementationClassPath")
    implementationClasspath.setAccessible(true)
    implementationClasspath
      .get(builder)
      .asInstanceOf[java.util.List[URL]]
