package org.podval.tools.test.detect

import org.gradle.api.{Action, GradleException}
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.TestFramework
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.TestFilter as TestFilterG
import org.gradle.api.tasks.testing.junit.JUnitOptions
import org.gradle.internal.Factory
import org.gradle.process.internal.worker.{DefaultWorkerProcessBuilder, WorkerProcessBuilder}
import org.podval.tools.build.{Output, TestEnvironment}
import org.podval.tools.test.detect.SbtTestFrameworkDetector
import org.podval.tools.test.filter.TestFilter
import org.podval.tools.test.run.RunTestDefinitionProcessorFactory
import org.podval.tools.util.{Classpath, Files, Reflection}
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava, SetHasAsScala}
import java.io.File
import java.net.URL

object SbtTestFramework:
  class Options extends JUnitOptions

class SbtTestFramework(
  defaultTestFilter: DefaultTestFilter,
  options: SbtTestFramework.Options,
  moduleRegistry: ModuleRegistry,
  testTaskTemporaryDir: Factory[File],
  dryRun: Provider[java.lang.Boolean],
  // delayed
  output: () => Output,
  testEnvironment: () => TestEnvironment[?]
) extends TestFramework:
  override def getDisplayName: String = "SBT Test Framework"

  override def getOptions: SbtTestFramework.Options = options

  override def copyWithFilters(newTestFilters: TestFilterG): SbtTestFramework =
    val copiedOptions: SbtTestFramework.Options = SbtTestFramework.Options()
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

  override def getProcessorFactory: RunTestDefinitionProcessorFactory[?] = RunTestDefinitionProcessorFactory(
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
    modules: List[String],
    jars: List[String]
  ): List[URL] =
    val classpath = Classpath.get
    jars
      .map(jar => classpath
        .find(_.getPath.contains(jar))
        .getOrElse(throw GradleException(s"Did not find artifact $jar on the classpath"))
      ) ++
    modules
      .map(moduleRegistry.getModule)
      .flatMap(_.getImplementationClasspath.getAsURLs.asScala)

  private def implementationClasspathAdditions: List[URL] = classPathAdditions(
    modules = List(
      // To have the freedom to place the code where I want without trying to segregate forkable code from un-forkable
      // and avoid initialization issues, I need to add some Gradle modules.
      //"gradle-core-api"
    ),
    jars = List(
      "org.podval.tools.scalajs",

      // Plugin itself is compiled with Scala 3,
      // so those parts of its code that run in a Scala 2 need to have access to the Scala 3 library, or execution fails
      // (on JVM only) with NoClassDefFoundError for things like scala/runtime/LazyVals$, scala.CanEqual etc.
      // Before Scala 3.8.0 all it took was to share 'scala3-library_3':
      //"scala3-library_3",
      // Starting with Scala 3.8.0 'scala3-library_3' is empty, so there is no point in sharing it;
      // everything is in `scala-library`, which is built with Scala 3, so we share that now:
      "scala-library"
      // it does not seem to break anything even on Scala.js and even on Scala 2.12 :)
    )
  )

  private def applicationClasspathAdditions: List[URL] = classPathAdditions(
    modules = List(
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

      // "groovy" module added to the applicationClasspath
      "org.codehaus.groovy"
    )

  private def getImplementationClasspath(builder: DefaultWorkerProcessBuilder): List[URL] = Reflection
    .Get[java.util.List[URL], DefaultWorkerProcessBuilder]("implementationClassPath")(builder)
    .asScala
    .toList

  override def getWorkerConfigurationAction: Action[WorkerProcessBuilder] = (builderInterface: WorkerProcessBuilder) =>
    val builder: DefaultWorkerProcessBuilder = builderInterface.asInstanceOf[DefaultWorkerProcessBuilder]

    builder.setImplementationClasspath(
      (getImplementationClasspath(builder) ++ implementationClasspathAdditions).asJava
    )

    builder.applicationClasspath(
      applicationClasspathAdditions.map(Files.url2file).asJava
    )

    builder.sharedPackages(sharedPackages.asJava)

    ()
