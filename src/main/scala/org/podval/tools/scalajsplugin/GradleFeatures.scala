package org.podval.tools.scalajsplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.artifacts.configurations.RoleBasedConfigurationContainerInternal
import org.gradle.api.{NamedDomainObjectProvider, Project, Task}
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.plugins.jvm.internal.{JvmFeatureInternal, JvmPluginServices}
import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.component.internal.DefaultJvmSoftwareComponent
import org.gradle.jvm.tasks.Jar
import org.gradle.testing.base.TestingExtension
import org.gradle.util.internal.GUtil
import org.podval.tools.build.Gradle
import org.podval.tools.scalajsplugin.gradle.{JavaPluginAsLibrary, ScalaBasePluginAsLibrary, ScalaPluginAsLibrary}

// The idea here is:
// for JVM: re-configure existing setup
// ("main" and "test" source sets, associated configurations and tasks)
// so that both JVM-specific and shared code is included;
// for a non-JVM backend: create a parallel setup
// ("main<backend>" and "test<backend>" source sets, associated configurations and tasks).
//
// When Scala plugin gets applied originally, it applies a number of other plugins.
// Some of them do non-source-set-specific things which do not need to be replicated:
// - BasePlugin;
// - JvmEcosystemPlugin;
// - ReportingBasePlugin;
// - JvmToolchainsPlugin.
// Some of them do source-set-specific things which do need to be replicated:
// - ScalaPlugin;
// - ScalaBasePlugin;
// - JavaPlugin.
//
// To replicate whatever needs to be replicated, the corresponding Gradle code was copied and adjusted to:
// - not assume that the only source sets that exist are "main" and "test";
// - use appropriate names for source sets, configurations, and tasks.
//
// It would be much better if this functionality was exposed by Gradle in one method call.
// It would be even better if there was a way to affect the class of the test task created.
// Since the chances of this ever happening are zero, I modified the code ;)
object GradleFeatures:
  private def scalaCompileTaskName(sourceSet: SourceSet): String = sourceSet.getCompileTaskName("scala")
  private def scaladocTaskName(sourceSet: SourceSet): String = sourceSet.getTaskName(null, "scaladoc")

  def configureShared(
    project: Project,
    sharedFeature: JvmFeatureInternal,
    sharedTestSuiteSourceSet: SourceSet
  ): Unit =
    disableSharedTasks(project, sharedFeature.getSourceSet)
    disableSharedTasks(project, sharedTestSuiteSourceSet)
    disableTask(project, sharedTestSuiteSourceSet.getName)

//    project.getTasks.register(linkTaskName(sharedFeature.getSourceSet))
    project.getTasks.register(GradleNames.runTaskName(sharedFeature.getSourceSet))

  private def disableTask(project: Project, sharedTaskName: String): Unit =
    // TODO use named()?
    val task: Task = project.getTasks.findByName(sharedTaskName)
    if task != null then task.setEnabled(false)

  private def disableSharedTasks(project: Project, sourceSet: SourceSet): Unit =
    def disable(taskName: SourceSet => String): Unit = disableTask(project, taskName(sourceSet))

    disable(_.getClassesTaskName)
    disable(_.getProcessResourcesTaskName)
    disable(_.getCompileJavaTaskName)
    disable(_.getJavadocTaskName)
    disable(_.getJarTaskName)
    disable(_.getJavadocJarTaskName)
    disable(_.getSourcesJarTaskName)
    disable(_.getSourcesJarTaskName)
    disable(scalaCompileTaskName)
    disable(scaladocTaskName)

  private def taskDependsOn(project: Project, sharedTaskName: String, taskName: String): Unit =
    val sharedTask: Task = project.getTasks.findByName(sharedTaskName)
    if sharedTask != null then sharedTask.dependsOn(taskName)

  private def sharedTasksDependOn(project: Project, sharedSourceSet: SourceSet, sourceSet: SourceSet): Unit =
    def dependsOn(taskName: SourceSet => String): Unit = taskDependsOn(project, taskName(sharedSourceSet), taskName(sourceSet))

    dependsOn(_.getClassesTaskName)
    dependsOn(_.getProcessResourcesTaskName)
    dependsOn(_.getCompileJavaTaskName)
    dependsOn(_.getJavadocTaskName)
    dependsOn(_.getJarTaskName)
    dependsOn(_.getJavadocJarTaskName)
    dependsOn(_.getSourcesJarTaskName)
    dependsOn(_.getSourcesJarTaskName)
    dependsOn(scalaCompileTaskName)
    dependsOn(scaladocTaskName)
//    dependsOn(linkTaskName)
    dependsOn(GradleNames.runTaskName)
  
  // TODO figure out which parts need to happen when pre-existing main feature needs to be turned into a JVM one...
  def createFeature(
    project: Project,
    configurations: RoleBasedConfigurationContainerInternal,
    component: DefaultJvmSoftwareComponent,
    testing: TestingExtension,
    sharedFeature: JvmFeatureInternal,
    sharedTestSuiteSourceSet: SourceSet,
    jvmPluginServices: JvmPluginServices,
    backendDisplayName: String,
    sourceRoot: String,
    sharedSourceRoot: String,
    mainSourceSetName: String,
    testSourceSetName: String
  ): Unit =
    // skip for pre-existing
    val feature: JvmFeatureInternal = JavaPluginAsLibrary.createFeature(
      project,
      component,
      featureName = mainSourceSetName, // feature and its main source set are named the same
      mainSourceSetName = mainSourceSetName
    )
    val mainSourceSet: SourceSet = feature.getSourceSet
    val scalaCompilerPluginsConfigurationName: String = GradleNames.scalaCompilerPluginsConfigurationName(mainSourceSet)
    val incrementalAnalysisUsageName: String = mainSourceSet.getTaskName("incremental-analysis", "")
    val incrementalAnalysisCategoryName: String = mainSourceSet.getTaskName("scala-analysis", "")
    val incrementalScalaAnalysisElementsConfigurationName: String = mainSourceSet.getTaskName("incrementalScalaAnalysisElements", "")

    // skip for pre-existing
    ScalaBasePluginAsLibrary.configureCompilerPluginsConfiguration(
      project,
      scalaCompilerPluginsConfigurationName,
      jvmPluginServices
    )
    // skip for pre-existing - or change the description of the configuration
    ScalaBasePluginAsLibrary.configureIncrementalAnalysisElements(
      project,
      incrementalAnalysisUsageName = incrementalAnalysisUsageName,
      incrementalAnalysisCategoryName = incrementalAnalysisCategoryName,
      incrementalScalaAnalysisElementsConfigurationName = incrementalScalaAnalysisElementsConfigurationName,
      backendDisplayName = backendDisplayName
    )
    // skip for pre-existing
    ScalaPluginAsLibrary.configureIncrementalAnalysisElements(
      project,
      compileTaskName = scalaCompileTaskName(mainSourceSet),
      incrementalScalaAnalysisElementsConfigurationName = incrementalScalaAnalysisElementsConfigurationName
    )
    ScalaPluginAsLibrary.configureScaladoc(
      project,
      isRegisterNotReplaceScaladocTask = true,
      sourceSet = mainSourceSet,
      backendDisplayName = backendDisplayName,
      scalaDocTaskName = scaladocTaskName(mainSourceSet)
    )
    
    val testSuite: NamedDomainObjectProvider[JvmTestSuite] = JavaPluginAsLibrary.createTestSuite(
      project,
      configurations,
      component,
      testing,
      feature,
      testSuiteName = testSourceSetName
    )
    val testSourceSet: SourceSet = testSuite.get.getSources
    
    ScalaBasePluginAsLibrary.configureSourceSetDefaults(
      project,
      sourceRoot = sourceRoot,
      sharedSourceRoot = sharedSourceRoot,
      mainSourceSetName = mainSourceSetName,
      testSourceSetName = testSourceSetName
    )
    ScalaBasePluginAsLibrary.configureCompileDefaults(
      project,
      scalaCompilerPluginsConfigurationName = scalaCompilerPluginsConfigurationName,
      mainScalaCompileTaskName = scalaCompileTaskName(mainSourceSet),
      testScalaCompileTaskName = scalaCompileTaskName(testSourceSet)
    )

    sharedTasksDependOn(project, sharedFeature.getSourceSet, mainSourceSet)

    // TODO more?
    def extendsFrom(configuration: JvmFeatureInternal => Configuration): Unit =
      configuration(feature).extendsFrom(configuration(sharedFeature))

    extendsFrom(_.getImplementationConfiguration)
    extendsFrom(_.getRuntimeOnlyConfiguration)
    extendsFrom(_.getCompileOnlyConfiguration)
    extendsFrom(_.getRuntimeClasspathConfiguration)

    testSuite.configure((suite: JvmTestSuite) =>
      val testSourceSet: SourceSet = suite.getSources

      sharedTasksDependOn(project, sharedTestSuiteSourceSet, testSourceSet)
      taskDependsOn(project, sharedTestSuiteSourceSet.getName, testSourceSet.getName)

      def extendsFrom(configurationName: SourceSet => String): Unit =
        configurations.getByName(configurationName(testSourceSet))
          .extendsFrom(configurations.getByName(configurationName(sharedTestSuiteSourceSet)))

      // TODO more?
      extendsFrom(_.getImplementationConfigurationName)
      extendsFrom(_.getRuntimeOnlyConfigurationName)
      extendsFrom(_.getCompileClasspathConfigurationName)
      extendsFrom(_.getRuntimeClasspathConfigurationName)
    )

  def configureJar(
    project: Project, 
    jarTaskName: String,
    archiveAppendixConvention: String
  ): Unit =
    project.getTasks.withType(classOf[Jar]).configureEach((jar: Jar) =>
      if jar.getName == jarTaskName then
        setArchiveJarConvention(jar, project)
        jar.getArchiveAppendix.convention(archiveAppendixConvention)
    )

  private def setArchiveJarConvention(jar: Jar, project: Project) =
    jar.getArchiveFileName.convention(project.provider(() =>
      // The only change: no dash before the appendix.
      // [baseName][appendix]-[version]-[classifier].[extension]
      var name: String = GUtil.elvis(jar.getArchiveBaseName.getOrNull, "")
      name += GUtil.elvis(jar.getArchiveAppendix.getOrNull, "")
      name += maybe(name, jar.getArchiveVersion.getOrNull)
      name += maybe(name, jar.getArchiveClassifier.getOrNull)

      val extension: String = jar.getArchiveExtension.getOrNull
      name += (if GUtil.isTrue(extension) then "." + extension else "")
      name
    ))

  private def maybe(prefix: String, value: String): String =
    if !GUtil.isTrue(value) then ""
    else if !GUtil.isTrue(prefix) then value
    else "-".concat(value)
