package org.podval.tools.scalajsplugin.gradle

import org.gradle.api.{Action, NamedDomainObjectProvider, Project, Task}
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.configurations.RoleBasedConfigurationContainerInternal
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.{ExtensionContainer, JavaBasePlugin, JavaPluginExtension, PluginContainer}
import org.gradle.api.plugins.internal.JavaConfigurationVariantMapping
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.plugins.jvm.internal.{DefaultJvmFeature, JvmFeatureInternal}
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.internal.PublicationInternal
import org.gradle.api.publish.internal.versionmapping.VersionMappingStrategyInternal
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.{SourceSet, SourceSetContainer, TaskContainer}
import org.gradle.jvm.component.internal.DefaultJvmSoftwareComponent
import org.gradle.testing.base.TestingExtension
import java.util.Collections

// Adopted from org.gradle.api.plugins.JavaPlugin.
object JavaPluginAsLibrary:
  def createFeature(
    project: Project,
    component: DefaultJvmSoftwareComponent,
    featureName: String,
    mainSourceSetName: String
  ): JvmFeatureInternal =
    val sourceSets: SourceSetContainer = project.getExtensions.getByType(classOf[JavaPluginExtension]).getSourceSets
    val sourceSet: SourceSet = sourceSets.create(mainSourceSetName)

    val feature: JvmFeatureInternal = DefaultJvmFeature(
      featureName,
      sourceSet,
      Collections.emptySet(),
      project.asInstanceOf[ProjectInternal],
      false,
      false
    )

    // Create a source directories variant for the feature
    feature.withSourceElements()

    component.getFeatures.add(feature)

    component.addVariantsFromConfiguration(
      feature.getApiElementsConfiguration,
      JavaConfigurationVariantMapping("compile", false, feature.getCompileClasspathConfiguration)
    )
    component.addVariantsFromConfiguration(
      feature.getRuntimeElementsConfiguration,
      JavaConfigurationVariantMapping("runtime", false, feature.getRuntimeClasspathConfiguration)
    )

    // TODO confirm that publications for the new feature are being created by the JavaBase plugin
    // and call configurePublishing() to affect ONLY the publications for this feature.
    //    configurePublishing(project.getPlugins, project.getExtensions, feature.getSourceSet)

    // This already happened
    // Set the 'java' component as the project's default.
    //    val defaultConfiguration: Configuration  = project.getConfigurations.getByName(Dependency.DEFAULT_CONFIGURATION)
    //    defaultConfiguration.extendsFrom(feature.getRuntimeElementsConfiguration)
    //    project.getComponents.asInstanceOf[SoftwareComponentContainerInternal].getMainComponent.convention(javaComponent)

    // Build the main jar when running `assemble`.
    //    val publicationSet: DefaultArtifactPublicationSet  = project.getExtensions.getByType(classOf[DefaultArtifactPublicationSet])
    //    publicationSet.addCandidateInternal(feature.getRuntimeElementsConfiguration.getArtifacts.iterator().next())

    feature

  def createTestSuite(
    project: Project,
    configurations: RoleBasedConfigurationContainerInternal,
    component: DefaultJvmSoftwareComponent,
    testing: TestingExtension,
    feature: JvmFeatureInternal,
    testSuiteName: String
  ): NamedDomainObjectProvider[JvmTestSuite] =
    val testSuite: NamedDomainObjectProvider[JvmTestSuite] = testing.getSuites.register(
      testSuiteName,
      classOf[JvmTestSuite],
      (suite: JvmTestSuite) =>
        val testSourceSet: SourceSet = suite.getSources

        val testImplementationConfiguration: Configuration = configurations.getByName(testSourceSet.getImplementationConfigurationName)
        val testRuntimeOnlyConfiguration: Configuration = configurations.getByName(testSourceSet.getRuntimeOnlyConfigurationName)
        val testCompileClasspathConfiguration: Configuration = configurations.getByName(testSourceSet.getCompileClasspathConfigurationName)
        val testRuntimeClasspathConfiguration: Configuration = configurations.getByName(testSourceSet.getRuntimeClasspathConfigurationName)

        val mainSourceSet: SourceSet = feature.getSourceSet
        val mainSourceSetOutput: FileCollection = mainSourceSet.getOutput
        val testSourceSetOutput: FileCollection = testSourceSet.getOutput
        testSourceSet.setCompileClasspath(project.getObjects.fileCollection.from(mainSourceSetOutput, testCompileClasspathConfiguration))
        testSourceSet.setRuntimeClasspath(project.getObjects.fileCollection.from(testSourceSetOutput, mainSourceSetOutput, testRuntimeClasspathConfiguration))

        testImplementationConfiguration.extendsFrom(configurations.getByName(mainSourceSet.getImplementationConfigurationName))
        testRuntimeOnlyConfiguration.extendsFrom(configurations.getByName(mainSourceSet.getRuntimeOnlyConfigurationName))
    )

    // Force the realization of this test suite, targets and task
    val suite: JvmTestSuite = testSuite.get()
    component.getTestSuites.add(suite)

    project.getTasks.named(
      JavaBasePlugin.CHECK_TASK_NAME,
      new Action[Task]:
        override def execute(task: Task): Unit = task.dependsOn(testSuite)
    )

    testSuite

  // TODO: This approach is not necessarily correct for non-main features. All publications will attempt to use the main feature's
  // compile and runtime classpaths for version mapping, even if a non-main feature is being published.
  private def configurePublishing(
    project: Project,
    plugins: PluginContainer, 
    sourceSet: SourceSet
  ): Unit = plugins.withType(
    classOf[PublishingPlugin],
    new Action[PublishingPlugin]:
      override def execute(plugin: PublishingPlugin): Unit =
        val publishing: PublishingExtension = project.getExtensions.getByType(classOf[PublishingExtension])
  
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
