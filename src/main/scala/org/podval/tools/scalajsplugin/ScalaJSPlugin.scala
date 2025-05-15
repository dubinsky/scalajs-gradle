package org.podval.tools.scalajsplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.artifacts.configurations.RoleBasedConfigurationContainerInternal
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JvmTestSuitePlugin
import org.gradle.api.plugins.internal.JavaPluginHelper
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.plugins.jvm.internal.{JvmFeatureInternal, JvmPluginServices}
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.{Plugin, Project}
import org.gradle.jvm.component.internal.DefaultJvmSoftwareComponent
import org.gradle.testing.base.TestingExtension
import org.gradle.util.internal.GUtil
import org.podval.tools.build.{AddConfigurationToClassPath, Gradle, GradleClassPath, ScalaBackendKind, ScalaLibrary, 
  ScalaPlatform}
import org.podval.tools.scalajsplugin.jvm.Jvm
import org.podval.tools.scalajsplugin.scalajs.ScalaJS
import org.podval.tools.scalajsplugin.scalanative.ScalaNative
import org.slf4j.{Logger, LoggerFactory}
import javax.inject.Inject

object ScalaJSPlugin:
  private val logger: Logger = LoggerFactory.getLogger(ScalaJSPlugin.getClass)

  val backendProperty: String = "org.podval.tools.scalajs.backend"

final class ScalaJSPlugin @Inject(
  jvmPluginServices: JvmPluginServices
) extends Plugin[Project]:
  override def apply(project: Project): Unit =
    project.getPluginManager.apply(classOf[ScalaPlugin])
    
    val nonJvmDelegates: Set[BackendDelegate[?]] = Set(
      // TODO include JVM here too
      ScalaJS,
      ScalaNative
    ) 
      .filter((delegate: BackendDelegate[?]) =>
        val file = project.file(delegate.sourceRoot)
        file.exists && file.isDirectory
      )

    val delegates: Set[BackendDelegate[?]] =
      if nonJvmDelegates.nonEmpty
      then nonJvmDelegates.incl(Jvm) // TODO do not include JVM here
      else Set(Option(project.findProperty(ScalaJSPlugin.backendProperty)).map(_.toString) match
        case None => Jvm
        case Some(name) => Set(Jvm, ScalaJS, ScalaNative)
          .find(_.backendKind.name == name)
          .getOrElse(throw IllegalArgumentException(s"Unknown backend '$name'."))
      )

    ScalaJSPlugin.logger.info(s"ScalaJSPlugin: running with ${delegates.map(_.backendKind.displayName).mkString(",")}.")

    val isModeMixed: Boolean = nonJvmDelegates.nonEmpty

    // returns the names of the main and test source sets
    // main source set name is also the name of the feature;
    // test source set name is also the name of the test suite and the test task
    def sourceSetNames(delegate: BackendDelegate[?]): (String, String) =
      if !isModeMixed
      // the only feature that exists
      then ("main", JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME) 
      else (delegate.sourceRoot, GUtil.toLowerCamelCase(s"${JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME} ${delegate.sourceRoot}"))
    
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
      .getByName(JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME)
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
          sourceRoot = delegate.sourceRoot,
          sharedSourceRoot = GradleNames.sharedSourceRoot,
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
