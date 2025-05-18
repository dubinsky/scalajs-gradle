package org.podval.tools.scalajsplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.artifacts.configurations.RoleBasedConfigurationContainerInternal
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.{JavaBasePlugin, JavaPluginExtension}
import org.gradle.api.plugins.internal.JavaPluginHelper
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.plugins.jvm.internal.{JvmFeatureInternal, JvmPluginServices}
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.{SourceSet, SourceSetContainer}
import org.gradle.api.{Plugin, Project}
import org.gradle.jvm.component.internal.DefaultJvmSoftwareComponent
import org.gradle.testing.base.TestingExtension
import org.podval.tools.build.jvm.JvmBackend
import org.podval.tools.build.{AddConfigurationToClassPath, Gradle, GradleClassPath, ScalaBackend, ScalaLibrary,
  ScalaPlatform}
import org.podval.tools.scalajsplugin.jvm.JvmDelegate
import org.podval.tools.test.task.TestTask
import org.slf4j.{Logger, LoggerFactory}
import java.io.File
import javax.inject.Inject

object ScalaJSPlugin:
  private val logger: Logger = LoggerFactory.getLogger(ScalaJSPlugin.getClass)

  val backendProperty: String = "org.podval.tools.scalajs.backend"

final class ScalaJSPlugin @Inject(
  jvmPluginServices: JvmPluginServices
) extends Plugin[Project]:
  override def apply(project: Project): Unit =
    project.getPluginManager.apply(classOf[ScalaPlugin])

    val (isModeMixed: Boolean, delegates) = Option(project.findProperty(ScalaJSPlugin.backendProperty))
      .map(_.toString)
      .map((name: String) => BackendDelegate.all
        .find(_.backend.name == name)
        .getOrElse(throw IllegalArgumentException(s"Unknown backend '$name'."))
      ) match
      case Some(backend) => (false, Set(backend))
      case None =>
        val presentDelegates: Set[BackendDelegate[?]] = BackendDelegate.all
          .filter((delegate: BackendDelegate[?]) =>
            val file: File = project.file(delegate.backend.sourceRoot)
            file.exists && file.isDirectory
        )
        if presentDelegates.nonEmpty
        then (true, presentDelegates)
        else (false, Set(JvmDelegate))

    ScalaJSPlugin.logger.info(s"ScalaJSPlugin: project '${project.getName}' running ${if isModeMixed then "mixed" else "single"} with ${delegates.map(_.backend.displayName).mkString(",")}.")

    val sharedSibling: File = File(project.getProjectDir.getParent, ScalaBackend.sharedSourceRoot)
    val sharedSiblingExists: Boolean = sharedSibling.exists() && sharedSibling.isDirectory
    
    project.getTasks.withType(classOf[TestTask]).configureEach((testTask: TestTask) =>
      testTask.setGroup(JavaBasePlugin.VERIFICATION_GROUP)
      testTask.useSbt()
    )

    val sourceSets: SourceSetContainer = project.getExtensions.getByType(classOf[JavaPluginExtension]).getSourceSets

    // returns the names of the main and test source sets
    // main source set name is also the name of the feature;
    // test source set name is also the name of the test suite and the test task
    def sourceSetNames(delegate: BackendDelegate[?]): (String, String) =
      if !isModeMixed
      // the only feature that exists
      then ("main", ScalaBackend.defaultTestSuiteName)
      else (delegate.backend.sourceRoot, delegate.backend.testSuiteName)

    def getSourceSets(delegate: BackendDelegate[?]): (SourceSet, SourceSet) =
      val (mainSourceSetName: String, testSourceSetName: String) = sourceSetNames(delegate)
      (
        sourceSets.getByName(mainSourceSetName),
        sourceSets.getByName(testSourceSetName)
      )

    val configurations: RoleBasedConfigurationContainerInternal = project.asInstanceOf[ProjectInternal].getConfigurations

    val component: DefaultJvmSoftwareComponent = JavaPluginHelper
      .getJavaComponent(project)
      .asInstanceOf[DefaultJvmSoftwareComponent]

    val testing: TestingExtension = project.getExtensions.findByType(classOf[TestingExtension])

    // Currently, I use pre-existing feature ("main") and test suite ("test") as shared;
    // this choice is not hard-coded in the code below on the off chance that I'd like
    // to experiment with the alternative approach: dedicate the pre-existing setup to JVM
    // and create a "shared" feature.
    // Of course, with the current approach, I do not do anything to the main feature/suite
    // when not running in the mixed mode; for the above experimentation, `GradleFeature`
    // code will need to provide `createSharedFeature()` and `configurePreExistingFeature()`.
    val sharedFeature: JvmFeatureInternal = component.getMainFeature
    val sharedTestSuiteSourceSet: SourceSet = testing
      .getSuites
      .getByName(ScalaBackend.defaultTestSuiteName)
      .asInstanceOf[JvmTestSuite]
      .getSources

    if isModeMixed then
      GradleFeatures.configureShared(
        project,
        sharedFeature,
        sharedTestSuiteSourceSet
      )

      for delegate: BackendDelegate[?] <- delegates do
        val (mainSourceSetName: String, testSourceSetName: String) = sourceSetNames(delegate)
        GradleFeatures.createFeature(
          project,
          configurations,
          component,
          testing,
          sharedFeature,
          sharedTestSuiteSourceSet,
          jvmPluginServices,
          backendDisplayName = delegate.backend.displayName,
          sourceRoot = delegate.backend.sourceRoot,
          sharedSourceRoot = ScalaBackend.sharedSourceRoot,
          mainSourceSetName = mainSourceSetName,
          testSourceSetName = testSourceSetName
        )

    delegates.foreach((delegate: BackendDelegate[?]) =>
      val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(delegate)

//      if sharedSiblingExists then GradleFeatures.addSharedSibling(sharedSibling, mainSourceSet, testSourceSet)

      // Create extension.
      delegate.createExtension.foreach(_.create(project))

      // Set 'runtimeClassPath' and dependency on the 'classes' task.
      project.getTasks.withType(delegate.taskClass).configureEach((task: BackendTask) =>
        def sourceSet: SourceSet = if task.isTest then testSourceSet else mainSourceSet
        if task.isInstanceOf[BackendTask.HasRuntimeClassPath] then
          task.asInstanceOf[BackendTask.HasRuntimeClassPath].getRuntimeClassPath.setFrom(sourceSet.getRuntimeClasspath)
        if task.isInstanceOf[BackendTask.DependsOnClasses] then
          task.asInstanceOf[BackendTask.DependsOnClasses].dependsOn(Gradle.getClassesTaskProvider(project, sourceSet))
      )

      // Create plugin dependencies configuration.
      delegate.pluginDependenciesConfigurationNameOpt.map(configurationName =>
        // TODO use new one-shot methods resolvable()/consumable() etc.
        val configuration: Configuration = project.getConfigurations.create(configurationName)
        configuration.setVisible(false)
        configuration.setCanBeConsumed(false)
        configuration.setDescription(s"${delegate.backend.displayName} dependencies used by the ScalaJS plugin.")
      )

      // Register tasks.
      delegate.registerTasks(
        project.getTasks,
        mainSourceSet = mainSourceSet,
        testSourceSet = testSourceSet
      )
    )

    project.afterEvaluate: (project: Project) =>
      val pluginScalaPlatform: ScalaPlatform =
        ScalaLibrary.getFromClasspath(GradleClassPath.collect(this)).toPlatform(JvmBackend)

      val projectScalaLibrary: ScalaLibrary = ScalaLibrary.getFromConfiguration(
        sharedFeature.getImplementationConfiguration
      )
      
      // Configure.
      delegates.foreach((delegate: BackendDelegate[?]) =>
        val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(delegate)
        delegate.configure(
          project,
          projectScalaLibrary,
          pluginScalaPlatform,
          mainSourceSet = mainSourceSet,
          testSourceSet = testSourceSet
        )
      )

      // Expand classpath.
      val addToClassPath: Set[AddConfigurationToClassPath] = delegates.flatMap((delegate: BackendDelegate[?]) =>
        val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(delegate)
        delegate.pluginDependenciesConfigurationNameOpt.map((pluginDependenciesConfigurationName: String) =>
          AddConfigurationToClassPath(
            project.getConfigurations.getByName(pluginDependenciesConfigurationName),
            project.getConfigurations.getByName(mainSourceSet.getRuntimeClasspathConfigurationName)
          )
        )
      )
      addToClassPath.foreach(_.add())
      addToClassPath.foreach(_.verify(projectScalaLibrary))
