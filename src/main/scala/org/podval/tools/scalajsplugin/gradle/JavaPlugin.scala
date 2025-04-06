package org.podval.tools.scalajsplugin.gradle

import org.gradle.api.{Action, NamedDomainObjectProvider, Project, Task}
import org.gradle.api.artifacts.{Configuration, Dependency}
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.configurations.RoleBasedConfigurationContainerInternal
import org.gradle.api.internal.component.SoftwareComponentContainerInternal
import org.gradle.api.internal.plugins.{DefaultArtifactPublicationSet, DslObject}
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.tasks.JvmConstants
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.{ExtensionContainer, JavaPluginExtension, JvmTestSuitePlugin, PluginContainer}
import org.gradle.api.plugins.internal.JavaConfigurationVariantMapping
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.plugins.jvm.internal.{DefaultJvmFeature, JvmFeatureInternal}
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.internal.PublicationInternal
import org.gradle.api.publish.internal.versionmapping.VersionMappingStrategyInternal
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.diagnostics.DependencyInsightReportTask
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.{SourceSet, SourceSetContainer, TaskCollection, TaskContainer}
import org.gradle.internal.execution.BuildOutputCleanupRegistry
import org.gradle.jvm.component.internal.{DefaultJvmSoftwareComponent, JvmSoftwareComponentInternal}
import org.gradle.jvm.tasks.Jar
import org.gradle.testing.base.TestingExtension
import org.podval.tools.scalajsplugin.GradleNames
import java.util.Collections
import java.util.concurrent.Callable

// see org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPlugin as Original

///**
// * <p>A {@link Plugin} which compiles and tests Java source, and assembles it into a JAR file.</p>
// *
// * This plugin creates a built-in {@link JvmTestSuite test suite} named {@code test} that represents the {@link Test} task for Java projects.
// *
// * @see <a href="https://docs.gradle.org/current/userguide/java_plugin.html">Java plugin reference</a>
// * @see <a href="https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html">JVM test suite plugin reference</a>
// */
final class JavaPlugin(
  gradleNames: GradleNames,
  project: Project
):
  def apply(): Unit =
    val projectInternal: ProjectInternal = project.asInstanceOf[ProjectInternal]

    // TODO JavaBasePlugin.apply()

    // TODO this was already done; redo?

    val sourceSets: SourceSetContainer = project.getExtensions.getByType(classOf[JavaPluginExtension]).getSourceSets

    val javaComponent: JvmSoftwareComponentInternal = createJavaComponent(projectInternal, sourceSets)

    configurePublishing(project.getPlugins, project.getExtensions, javaComponent.getMainFeature.getSourceSet)

    // Set the 'java' component as the project's default.
    val defaultConfiguration: Configuration  = project.getConfigurations.getByName(Dependency.DEFAULT_CONFIGURATION)
    defaultConfiguration.extendsFrom(javaComponent.getMainFeature.getRuntimeElementsConfiguration)
    project.getComponents.asInstanceOf[SoftwareComponentContainerInternal].getMainComponent.convention(javaComponent)

    // Build the main jar when running `assemble`.
    val publicationSet: DefaultArtifactPublicationSet  = project.getExtensions.getByType(classOf[DefaultArtifactPublicationSet])
    // TODO ? publicationSet.addCandidateInternal(javaComponent.getMainFeature.getRuntimeElementsConfiguration.getArtifacts.iterator().next())

    val buildOutputCleanupRegistry: BuildOutputCleanupRegistry = projectInternal.getServices.get(classOf[BuildOutputCleanupRegistry])
    configureSourceSets(buildOutputCleanupRegistry, sourceSets)

    configureTestTaskOrdering(project.getTasks)
    configureDiagnostics(javaComponent.getMainFeature)
    configureBuild()

  private def createMainFeature(project: ProjectInternal, sourceSets: SourceSetContainer): JvmFeatureInternal =
    val sourceSet: SourceSet = sourceSets.create(gradleNames.mainSourceSetName)

    val feature: JvmFeatureInternal = DefaultJvmFeature(
      JvmConstants.JAVA_MAIN_FEATURE_NAME,
      sourceSet,
      Collections.emptySet(),
      project,
      false,
      false
    )

    // Create a source directories variant for the feature
    feature.withSourceElements()

    feature

  private def createJavaComponent(project: ProjectInternal, sourceSets: SourceSetContainer): JvmSoftwareComponentInternal =
    val component: DefaultJvmSoftwareComponent = project.getObjects.newInstance(
      classOf[DefaultJvmSoftwareComponent], 
      JvmConstants.JAVA_MAIN_COMPONENT_NAME
    )
    
    project.getComponents.add(component)

    // Create the main feature
    val mainFeature: JvmFeatureInternal = createMainFeature(project, sourceSets)
    component.getFeatures.add(mainFeature)

    // TODO: This process of manually adding variants to the component should be handled automatically when adding the feature to the component.
    component.addVariantsFromConfiguration(
      mainFeature.getApiElementsConfiguration, 
      JavaConfigurationVariantMapping("compile", false, mainFeature.getCompileClasspathConfiguration)
    )
    component.addVariantsFromConfiguration(
      mainFeature.getRuntimeElementsConfiguration,
      JavaConfigurationVariantMapping("runtime", false, mainFeature.getRuntimeClasspathConfiguration)
    )

    // Create the default test suite
    val defaultTestSuite: JvmTestSuite = createDefaultTestSuite(
      mainFeature,
      project.getConfigurations,
      project.getTasks, 
      project.getExtensions,
      project.getObjects
    )
    
    component.getTestSuites.add(defaultTestSuite)

    component

  // TODO: This approach is not necessarily correct for non-main features. All publications will attempt to use the main feature's
  // compile and runtime classpaths for version mapping, even if a non-main feature is being published.
  private def configurePublishing(
    plugins: PluginContainer, 
    extensions: ExtensionContainer,
    sourceSet: SourceSet
  ): Unit =
    plugins.withType(
      classOf[PublishingPlugin],
      new Action[PublishingPlugin]:
        override def execute(plugin: PublishingPlugin): Unit =
          val publishing: PublishingExtension = extensions.getByType(classOf[PublishingExtension])
    
          // Set up the default configurations used when mapping to resolved versions
          publishing.getPublications.withType(
            classOf[IvyPublication],
            new Action[IvyPublication]:
              override def execute(publication: IvyPublication): Unit =
                val strategy: VersionMappingStrategyInternal = publication.asInstanceOf[PublicationInternal[?]].getVersionMappingStrategy
                strategy.defaultResolutionConfiguration(Usage.JAVA_API, sourceSet.getCompileClasspathConfigurationName)
                strategy.defaultResolutionConfiguration(Usage.JAVA_RUNTIME, sourceSet.getRuntimeClasspathConfigurationName)
          )
          publishing.getPublications.withType(
            classOf[MavenPublication], 
            new Action[MavenPublication]:
              override def execute(publication: MavenPublication): Unit =
                val strategy: VersionMappingStrategyInternal = publication.asInstanceOf[PublicationInternal[?]].getVersionMappingStrategy
                strategy.defaultResolutionConfiguration(Usage.JAVA_API, sourceSet.getCompileClasspathConfigurationName)
                strategy.defaultResolutionConfiguration(Usage.JAVA_RUNTIME, sourceSet.getRuntimeClasspathConfigurationName)
          )
    )

  private def configureSourceSets(
    buildOutputCleanupRegistry: BuildOutputCleanupRegistry, 
    sourceSets: SourceSetContainer
  ): Unit =
    // Register the project's source set output directories
    sourceSets.all(
      new Action[SourceSet]:
        override def execute(sourceSet: SourceSet): Unit = buildOutputCleanupRegistry.registerOutputs(sourceSet.getOutput)
    )

  /**
   * Unless there are other concerns, we'd prefer to run jar tasks prior to test tasks, as this might offer a small performance improvement
   * for common usage.  In practice, running test tasks tends to take longer than building a jar; especially as a project matures. If tasks
   * in downstream projects require the jar from this project, and the jar and test tasks in this project are available to be run in either order,
   * running jar first so that other projects can continue executing tasks in parallel while this project runs its tests could be an improvement.
   * However, while we want to prioritize cross-project dependencies to maximize parallelism if possible, we don't want to add an explicit
   * dependsOn() relationship between the jar task and the test task, so that any projects which need to run test tasks first will not need modification.
   */
  private def configureTestTaskOrdering(tasks: TaskContainer): Unit =
    val jarTasks: TaskCollection[Jar] = tasks.withType(classOf[Jar])
    tasks.withType(classOf[Test]).configureEach(test => test.shouldRunAfter(jarTasks))

  private def createDefaultTestSuite(
    mainFeature: JvmFeatureInternal,
    configurations: RoleBasedConfigurationContainerInternal,
    tasks: TaskContainer,
    extensions: ExtensionContainer,
    objectFactory: ObjectFactory
  ): JvmTestSuite =
    val testing: TestingExtension = extensions.findByType(classOf[TestingExtension])
    val testSuite: NamedDomainObjectProvider[JvmTestSuite] = testing.getSuites.register(
      JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME,
      classOf[JvmTestSuite],
      new Action[JvmTestSuite]:
        override def execute(suite: JvmTestSuite): Unit =
          val testSourceSet: SourceSet = suite.getSources
  
          val testImplementationConfiguration: Configuration = configurations.getByName(testSourceSet.getImplementationConfigurationName)
          val testRuntimeOnlyConfiguration: Configuration = configurations.getByName(testSourceSet.getRuntimeOnlyConfigurationName)
          val testCompileClasspathConfiguration: Configuration = configurations.getByName(testSourceSet.getCompileClasspathConfigurationName)
          val testRuntimeClasspathConfiguration: Configuration = configurations.getByName(testSourceSet.getRuntimeClasspathConfigurationName)
  
          // We cannot reference the main source set lazily (via a callable) since the IntelliJ model builder
          // relies on the main source set being created before the tests. So, this code here cannot live in the
          // JvmTestSuitePlugin and must live here, so that we can ensure we register this test suite after we've
          // created the main source set.
          val mainSourceSet: SourceSet = mainFeature.getSourceSet
          val mainSourceSetOutput: FileCollection = mainSourceSet.getOutput
          val testSourceSetOutput: FileCollection = testSourceSet.getOutput
          testSourceSet.setCompileClasspath(objectFactory.fileCollection().from(mainSourceSetOutput, testCompileClasspathConfiguration))
          testSourceSet.setRuntimeClasspath(objectFactory.fileCollection().from(testSourceSetOutput, mainSourceSetOutput, testRuntimeClasspathConfiguration))
  
          testImplementationConfiguration.extendsFrom(configurations.getByName(mainSourceSet.getImplementationConfigurationName))
          testRuntimeOnlyConfiguration.extendsFrom(configurations.getByName(mainSourceSet.getRuntimeOnlyConfigurationName))
    )

    // Force the realization of this test suite, targets and task
    val suite: JvmTestSuite = testSuite.get()

    tasks.named(JavaBasePlugin.CHECK_TASK_NAME, task => task.dependsOn(testSuite))

    suite

  private def configureDiagnostics(mainFeature: JvmFeatureInternal): Unit =
    project.getTasks.withType(classOf[DependencyInsightReportTask]).configureEach(task =>
      DslObject(task).getConventionMapping.map("configuration", () => mainFeature.getCompileClasspathConfiguration)
    )

  private def configureBuild(): Unit =
    project.getTasks.named(
      JavaBasePlugin.BUILD_NEEDED_TASK_NAME, task => addDependsOnTaskInOtherProjects(
        task, 
        true,
        JavaBasePlugin.BUILD_NEEDED_TASK_NAME, 
        JvmConstants.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME
      )
    )
    
    project.getTasks.named(
      JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME,
      task => addDependsOnTaskInOtherProjects(
        task, 
        false,
        JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME,
        JvmConstants.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME
      )
    )

  /**
   * Adds a dependency on tasks with the specified name in other projects.  The other projects are determined from
   * project lib dependencies using the specified configuration name. These may be projects this project depends on or
   * projects that depend on this project based on the useDependOn argument.
   *
   * @param task Task to add dependencies to
   * @param useDependedOn if true, add tasks from projects this project depends on, otherwise use projects that depend on this one.
   * @param otherProjectTaskName name of task in other projects
   * @param configurationName name of configuration to use to find the other projects
   */
  private def addDependsOnTaskInOtherProjects(
    task: Task, 
    useDependedOn: Boolean, 
    otherProjectTaskName: String, 
    configurationName: String
  ) : Unit =
    val project: Project = task.getProject
    val configuration: Configuration = project.getConfigurations.getByName(configurationName)
    task.dependsOn(configuration.getTaskDependencyFromProjectDependency(useDependedOn, otherProjectTaskName))
    
