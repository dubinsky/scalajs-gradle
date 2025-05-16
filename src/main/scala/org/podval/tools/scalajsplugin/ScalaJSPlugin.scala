package org.podval.tools.scalajsplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.artifacts.configurations.RoleBasedConfigurationContainerInternal
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.internal.JavaPluginHelper
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.plugins.jvm.internal.{JvmFeatureInternal, JvmPluginServices}
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.{Plugin, Project}
import org.gradle.jvm.component.internal.DefaultJvmSoftwareComponent
import org.gradle.testing.base.TestingExtension
import org.podval.tools.build.{AddConfigurationToClassPath, Gradle, GradleClassPath, ScalaBackendKind, ScalaLibrary,
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

    project.getTasks.withType(classOf[TestTask]).configureEach((testTask: TestTask) =>
      testTask.setGroup(JavaBasePlugin.VERIFICATION_GROUP)
      testTask.useSbt()
    )

    val presentDelegates: Set[BackendDelegate[?]] = BackendDelegate.all
      .filter((delegate: BackendDelegate[?]) =>
        val file: File = project.file(delegate.backendKind.sourceRoot)
        file.exists && file.isDirectory
      )

    val isModeMixed: Boolean = presentDelegates.nonEmpty

    val delegates: Set[BackendDelegate[?]] =
      if presentDelegates.nonEmpty
      then presentDelegates
      else Set(Option(project.findProperty(ScalaJSPlugin.backendProperty)).map(_.toString) match
        case None => JvmDelegate
        case Some(name) => BackendDelegate
          .all
          .find(_.backendKind.name == name)
          .getOrElse(throw IllegalArgumentException(s"Unknown backend '$name'."))
      )

    ScalaJSPlugin.logger.info(s"ScalaJSPlugin: running with ${delegates.map(_.backendKind.displayName).mkString(",")}.")
    
    // returns the names of the main and test source sets
    // main source set name is also the name of the feature;
    // test source set name is also the name of the test suite and the test task
    def sourceSetNames(delegate: BackendDelegate[?]): (String, String) =
      if !isModeMixed
      // the only feature that exists
      then ("main", ScalaBackendKind.defaultTestSuiteName)
      else (delegate.backendKind.sourceRoot, delegate.backendKind.testSuiteName)
    
    def getSourceSets(delegate: BackendDelegate[?]): (SourceSet, SourceSet) =
      val (mainSourceSetName: String, testSourceSetName: String) = sourceSetNames(delegate)
      (
        Gradle.getSourceSet(project, mainSourceSetName),
        Gradle.getSourceSet(project, testSourceSetName)
      )

    // TODO get the source sets here!
    
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
      .getByName(ScalaBackendKind.defaultTestSuiteName)
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
          backendDisplayName = delegate.backendKind.displayName,
          sourceRoot = delegate.backendKind.sourceRoot,
          sharedSourceRoot = ScalaBackendKind.sharedSourceRoot,
          mainSourceSetName = mainSourceSetName,
          testSourceSetName = testSourceSetName
        )

    delegates.foreach((delegate: BackendDelegate[?]) =>
      val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(delegate)
 
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
        configuration.setDescription(s"${delegate.backendKind.displayName} dependencies used by the ScalaJS plugin.")
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
        ScalaLibrary.getFromClasspath(GradleClassPath.collect(this)).toPlatform(ScalaBackendKind.JVM)

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
