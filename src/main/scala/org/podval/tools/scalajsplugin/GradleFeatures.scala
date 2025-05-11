package org.podval.tools.scalajsplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.artifacts.configurations.RoleBasedConfigurationContainerInternal
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JvmTestSuitePlugin
import org.gradle.api.{Action, NamedDomainObjectProvider, Project, Task}
import org.gradle.api.plugins.internal.JavaPluginHelper
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.plugins.jvm.internal.{JvmFeatureInternal, JvmPluginServices}
import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.component.internal.DefaultJvmSoftwareComponent
import org.gradle.jvm.tasks.Jar
import org.gradle.testing.base.TestingExtension
import org.podval.tools.build.Gradle
import org.podval.tools.scalajsplugin.gradle.{JavaPluginAsLibrary, ScalaBasePluginAsLibrary, ScalaPluginAsLibrary}

object GradleFeatures:
  private def scalaCompileTaskName(sourceSet: SourceSet): String = sourceSet.getCompileTaskName("scala")
  private def scaladocTaskName(sourceSet: SourceSet): String = sourceSet.getTaskName(null, "scaladoc")
  
  def configure(
    project: Project,
    jvmPluginServices: JvmPluginServices,
    isModeMixed: Boolean,
    bindings: Set[BackendDelegateBinding]
  ): String =
    val configurations: RoleBasedConfigurationContainerInternal = project.asInstanceOf[ProjectInternal].getConfigurations

    val component: DefaultJvmSoftwareComponent = JavaPluginHelper
      .getJavaComponent(project)
      .asInstanceOf[DefaultJvmSoftwareComponent]

    val testing: TestingExtension = project.getExtensions.findByType(classOf[TestingExtension])
    
    val sharedFeature: JvmFeatureInternal = component.getMainFeature

    val sharedTestSuiteSourceSet: SourceSet = testing
      .getSuites
      .getByName(JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME)
      .asInstanceOf[JvmTestSuite]
      .getSources

    for binding: BackendDelegateBinding <- bindings do configure(
      project,
      configurations,
      component,
      testing,
      sharedFeature,
      sharedTestSuiteSourceSet,
      jvmPluginServices,
      isModeMixed,
      binding.delegate,
      binding.gradleNames
    )

    sharedFeature.getImplementationConfiguration.getName

  private def configure(
    project: Project,
    configurations: RoleBasedConfigurationContainerInternal,
    component: DefaultJvmSoftwareComponent,
    testing: TestingExtension,
    sharedFeature: JvmFeatureInternal,
    sharedTestSuiteSourceSet: SourceSet,
    jvmPluginServices: JvmPluginServices,
    isModeMixed: Boolean,
    delegate: BackendDelegate,
    gradleNames: GradleNames
  ): Unit =
    val isCreate: Boolean = isModeMixed
    val backendDisplayName = delegate.backendKind.displayName
    val incrementalScalaAnalysisElementsConfigurationName: String = gradleNames.suffix("incrementalScalaAnalysisElements")

    val feature: JvmFeatureInternal =
      if !isCreate
      then component.getFeatures.findByName(gradleNames.mainSourceSetName)
      else JavaPluginAsLibrary.createFeature(
        project,
        component,
        featureName = gradleNames.mainSourceSetName,
        mainSourceSetName = gradleNames.mainSourceSetName
      )

    if isCreate then
      val testSuite: NamedDomainObjectProvider[JvmTestSuite] = JavaPluginAsLibrary.createTestSuite(
        project,
        configurations,
        component,
        testing,
        feature,
        testSuiteName = gradleNames.testSourceSetName
      )
      configureTestSuite(
        project,
        testSuite,
        sharedTestSuiteSourceSet,
        configurations
      )

    if isCreate then
      ScalaBasePluginAsLibrary.configureCompilerPluginsConfiguration(
        project,
        gradleNames.scalaCompilerPluginsConfigurationName,
        jvmPluginServices
      )
      ScalaBasePluginAsLibrary.configureIncrementalAnalysisElements(
        project,
        incrementalAnalysisUsageName = gradleNames.suffix("incremental-analysis"),
        incrementalAnalysisCategoryName = gradleNames.suffix("scala-analysis"),
        incrementalScalaAnalysisElementsConfigurationName = incrementalScalaAnalysisElementsConfigurationName,
        backendDisplayName = backendDisplayName
      )
      ScalaBasePluginAsLibrary.configureCompileDefaults(
        project,
        scalaCompilerPluginsConfigurationName = gradleNames.scalaCompilerPluginsConfigurationName,
        mainScalaCompileTaskName = scalaCompileTaskName(Gradle.getSourceSet(project, gradleNames.testSourceSetName)),
        testScalaCompileTaskName = backendDisplayName
      )

    ScalaBasePluginAsLibrary.configureSourceSetDefaults(
      project,
      sourceRoot = delegate.sourceRoot,
      sharedSourceRoot = GradleNames.sharedSourceRoot,
      mainSourceSetName = gradleNames.mainSourceSetName,
      testSourceSetName = gradleNames.testSourceSetName
    )

    ScalaPluginAsLibrary.configureScaladoc(
      project,
      isCreate,
      feature.getSourceSet,
      backendDisplayName = backendDisplayName,
      scalaDocTaskName = scaladocTaskName(feature.getSourceSet)
    )
    ScalaPluginAsLibrary.configureIncrementalAnalysisElements(
      project,
      compileTaskName = scalaCompileTaskName(feature.getSourceSet),
      incrementalScalaAnalysisElementsConfigurationName = incrementalScalaAnalysisElementsConfigurationName
    )

    if isCreate then configureFeature(
      project,
      feature,
      sharedFeature
    )

  private def configureFeature(
    project: Project,
    feature: JvmFeatureInternal,
    sharedFeature: JvmFeatureInternal
  ): Unit =
    // TODO more?
    def extendsFrom(configuration: JvmFeatureInternal => Configuration): Unit =
      configuration(feature).extendsFrom(configuration(sharedFeature))

    extendsFrom(_.getImplementationConfiguration  )
    extendsFrom(_.getRuntimeOnlyConfiguration     )
    extendsFrom(_.getCompileOnlyConfiguration     )
    extendsFrom(_.getRuntimeClasspathConfiguration)

    // TODO if named() is lazy, use it her and in the XXXPluginAsLibrary...
    def sharedTaskDependsOn(taskName: SourceSet => String): Unit =
      val task: Task = project.getTasks.findByName(taskName(sharedFeature.getSourceSet))
      if task != null then
        task.setEnabled(false)
        task.dependsOn(taskName(feature.getSourceSet))

    // Ignoring Java tasks: `compileJava, javadoc, javadocJar`.
    sharedTaskDependsOn(_.getClassesTaskName)
    sharedTaskDependsOn(_.getProcessResourcesTaskName)
    sharedTaskDependsOn(_.getJarTaskName)
    sharedTaskDependsOn(_.getSourcesJarTaskName)
    // TODO add scaladoc, link and run tasks!

    project.getTasks.withType(classOf[Jar]).configureEach((jar: Jar) =>
      if jar.getName == feature.getSourceSet.getJarTaskName then
        // TODO redo archiveName's convention to format the name properly;
        // pass enough here to set the Scala version and the suffix...
        jar.getArchiveAppendix.convention(feature.getName)
    )

  private def configureTestSuite(
    project: Project,
    testSuite: NamedDomainObjectProvider[JvmTestSuite],
    sharedTestSuiteSourceSet: SourceSet,
    configurations: RoleBasedConfigurationContainerInternal
  ): Unit =
    testSuite.configure((suite: JvmTestSuite) =>
      val testSourceSet: SourceSet = suite.getSources

      def extendsFrom(configurationName: SourceSet => String): Unit =
        configurations.getByName(configurationName(testSourceSet))
          .extendsFrom(configurations.getByName(configurationName(sharedTestSuiteSourceSet)))

      // TODO more?
      extendsFrom(_.getImplementationConfigurationName  )
      extendsFrom(_.getRuntimeOnlyConfigurationName     )
      extendsFrom(_.getCompileClasspathConfigurationName)
      extendsFrom(_.getRuntimeClasspathConfigurationName)

      // TODO if named() is lazy, use it her and in the XXXPluginAsLibrary...
      def sharedTaskDependsOn(taskName: SourceSet => String): Unit =
        val task: Task = project.getTasks.getByName(taskName(sharedTestSuiteSourceSet))
        task.setEnabled(false)
        task.dependsOn(taskName(suite.getSources))

      sharedTaskDependsOn(_.getClassesTaskName)
      sharedTaskDependsOn(_.getProcessResourcesTaskName)
      // TODO compileScala and testLink tasks!
    )

    // TODO looks like I do not need this?
//    val sharedTestTask: Task = project.getTasks.getByName(sharedTestSuiteSourceSet.getName)
//    sharedTestTask.setEnabled(false)
//    sharedTestTask.dependsOn(testSuite)
