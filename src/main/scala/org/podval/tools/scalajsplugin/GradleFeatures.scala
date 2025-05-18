package org.podval.tools.scalajsplugin

import org.gradle.api.artifacts.{ConfigurablePublishArtifact, Configuration}
import org.gradle.api.attributes.{Category, Usage}
import org.gradle.api.file.{ConfigurableFileCollection, RegularFile}
import org.gradle.api.internal.artifacts.configurations.{ConfigurationRolesForMigration, RoleBasedConfigurationContainerInternal}
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.{Action, NamedDomainObjectProvider, Project, Task}
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.plugins.jvm.internal.{JvmFeatureInternal, JvmPluginServices}
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.scala.{ScalaCompile, ScalaDoc}
import org.gradle.api.tasks.{ScalaSourceDirectorySet, SourceSet, TaskProvider}
import org.gradle.jvm.component.internal.DefaultJvmSoftwareComponent
import org.gradle.jvm.tasks.Jar
import org.gradle.language.scala.tasks.AbstractScalaCompile
import org.gradle.testing.base.TestingExtension
import org.gradle.util.internal.GUtil
import org.podval.tools.build.Gradle
import org.podval.tools.scalajsplugin.gradle.JavaPluginAsLibrary
import scala.jdk.CollectionConverters.*
import java.io.File

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
  def linkTaskName(sourceSet: SourceSet): String = sourceSet.getTaskName("link", "")
  def runTaskName(sourceSet: SourceSet): String = sourceSet.getTaskName("run", "")
  private def scalaCompileTaskName(sourceSet: SourceSet): String = sourceSet.getCompileTaskName("scala")
  private def scaladocTaskName(sourceSet: SourceSet): String = sourceSet.getTaskName(null, "scaladoc")

  def scalaCompilerPluginsConfigurationName(sourceSet: SourceSet): String =
    sourceSet.getTaskName(ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME, "")

  def configureShared(
    project: Project,
    sharedFeature: JvmFeatureInternal,
    sharedTestSuiteSourceSet: SourceSet
  ): Unit =
    disableSharedTasks(project, sharedFeature.getSourceSet)
    disableSharedTasks(project, sharedTestSuiteSourceSet)
    disableTask(project, sharedTestSuiteSourceSet.getName)

//    project.getTasks.register(linkTaskName(sharedFeature.getSourceSet))
    project.getTasks.register(runTaskName(sharedFeature.getSourceSet))

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
    dependsOn(runTaskName)
  
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
    val scalaCompilerPluginsConfigurationName: String = GradleFeatures.scalaCompilerPluginsConfigurationName(mainSourceSet)
    val incrementalAnalysisUsageName: String = mainSourceSet.getTaskName("incremental-analysis", "")
    val incrementalAnalysisCategoryName: String = mainSourceSet.getTaskName("scala-analysis", "")
    val incrementalScalaAnalysisElementsConfigurationName: String = mainSourceSet.getTaskName("incrementalScalaAnalysisElements", "")

    // skip for pre-existing
    configureCompilerPluginsConfiguration(
      project,
      scalaCompilerPluginsConfigurationName,
      jvmPluginServices
    )
    // skip for pre-existing - or change the description of the configuration
    configureIncrementalAnalysisElements(
      project,
      incrementalAnalysisUsageName = incrementalAnalysisUsageName,
      incrementalAnalysisCategoryName = incrementalAnalysisCategoryName,
      incrementalScalaAnalysisElementsConfigurationName = incrementalScalaAnalysisElementsConfigurationName,
      backendDisplayName = backendDisplayName
    )
    // skip for pre-existing
    configureIncrementalAnalysisElements(
      project,
      compileTaskName = scalaCompileTaskName(mainSourceSet),
      incrementalScalaAnalysisElementsConfigurationName = incrementalScalaAnalysisElementsConfigurationName
    )
    configureScaladoc(
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
    
    configureSourceRoots(
      project,
      mainSourceSet = mainSourceSet,
      testSourceSet = testSourceSet,
      sourceRoots = Seq(
        s"$sourceRoot/src",
        s"$sharedSourceRoot/src",
        s"src"
      )
    )

    configureCompileDefaults(
      project,
      scalaCompilerPluginsConfigurationName = scalaCompilerPluginsConfigurationName,
      mainSourceSet = mainSourceSet,
      testSourceSet = testSourceSet
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

  // Adopted from org.gradle.api.plugins.scala.ScalaBasePlugin.
  private def configureCompilerPluginsConfiguration(
    project: Project,
    scalaCompilerPluginsConfigurationName: String,
    jvmPluginServices: JvmPluginServices
  ): Unit =
    val plugins: Configuration = project.asInstanceOf[ProjectInternal].getConfigurations.resolvableDependencyScopeUnlocked(
      scalaCompilerPluginsConfigurationName
    )
    plugins.setTransitive(false)
    jvmPluginServices.configureAsRuntimeClasspath(plugins)
  
  def addSharedSibling(
    sharedSibling: File,
    mainSourceSet: SourceSet,
    testSourceSet: SourceSet
  ): Unit =
    def addSharedSourceRoots(sourceSet: SourceSet, srcDirectory: String): Unit =
      val toAdd: File = File(File(sharedSibling, srcDirectory), "scala")
      println(s"adding $toAdd")
      sourceSet
      .getExtensions
      .getByType(classOf[ScalaSourceDirectorySet])
      .srcDir(toAdd)

    addSharedSourceRoots(mainSourceSet, srcDirectory = "main")
    addSharedSourceRoots(testSourceSet, srcDirectory = "test")


  // Adopted from org.gradle.api.plugins.scala.ScalaBasePlugin.
  private def configureSourceRoots(
    project: Project,
    sourceRoots: Seq[String],
    mainSourceSet: SourceSet,
    testSourceSet: SourceSet
  ): Unit =
    def configureSourceSetDefaults(sourceSet: SourceSet, srcDirectory: String): Unit = sourceSet
      .getExtensions
      .getByType(classOf[ScalaSourceDirectorySet])
      .setSrcDirs(sourceRoots.map(sourceRoot => s"$sourceRoot/$srcDirectory/scala").map(project.file).asJava)

    configureSourceSetDefaults(mainSourceSet, srcDirectory = "main")
    configureSourceSetDefaults(testSourceSet, srcDirectory = "test")

  // Adopted from org.gradle.api.plugins.scala.ScalaBasePlugin.
  private def configureCompileDefaults(
    project: Project,
    scalaCompilerPluginsConfigurationName: String,
    mainSourceSet: SourceSet,
    testSourceSet: SourceSet
  ): Unit =
    def configureCompileDefaults(sourceSet: SourceSet): Unit =
      val scalaCompileTaskName: String = GradleFeatures.scalaCompileTaskName(sourceSet)
      project.getTasks.withType(classOf[ScalaCompile]).configureEach(compile =>
        if compile.getName == scalaCompileTaskName then compile.getConventionMapping.map(
          "scalaCompilerPlugins",
          () => project.getConfigurations.getAt(scalaCompilerPluginsConfigurationName)
        )
      )

    configureCompileDefaults(mainSourceSet)
    configureCompileDefaults(testSourceSet)

  // Adopted from org.gradle.api.plugins.scala.ScalaBasePlugin.
  private def configureIncrementalAnalysisElements(
    project: Project,
    incrementalAnalysisUsageName: String,
    incrementalAnalysisCategoryName: String,
    incrementalScalaAnalysisElementsConfigurationName: String,
    backendDisplayName: String
  ): Unit =
    val incrementalAnalysisUsage: Usage = project.getObjects.named(classOf[Usage], incrementalAnalysisUsageName)
    val incrementalAnalysisCategory: Category = project.getObjects.named(classOf[Category], incrementalAnalysisCategoryName)

    val incrementalAnalysisElements: Configuration = project.asInstanceOf[ProjectInternal].getConfigurations.migratingUnlocked(
      incrementalScalaAnalysisElementsConfigurationName,
      ConfigurationRolesForMigration.CONSUMABLE_DEPENDENCY_SCOPE_TO_CONSUMABLE
    )
    incrementalAnalysisElements.setVisible(false)
    incrementalAnalysisElements.setDescription(s"Incremental compilation analysis files for $backendDisplayName")
    incrementalAnalysisElements.getAttributes.attribute(Usage.USAGE_ATTRIBUTE, incrementalAnalysisUsage)
    incrementalAnalysisElements.getAttributes.attribute(Category.CATEGORY_ATTRIBUTE, incrementalAnalysisCategory)

  // Adopted from org.gradle.api.plugins.scala.ScalaPlugin.
  private def configureIncrementalAnalysisElements(
    project: Project,
    compileTaskName: String,
    incrementalScalaAnalysisElementsConfigurationName: String
  ): Unit =
    // TODO and for the test source set?
    val incrementalAnalysisElements: Configuration = project.getConfigurations.getByName(incrementalScalaAnalysisElementsConfigurationName)
    val compileScalaMapping: Provider[RegularFile] = project.getLayout.getBuildDirectory.file(s"tmp/scala/compilerAnalysis/$compileTaskName.mapping")
    val compileScala: TaskProvider[AbstractScalaCompile] = project.getTasks.withType(classOf[AbstractScalaCompile]).named(compileTaskName)
    compileScala.configure(_.getAnalysisMappingFile.set(compileScalaMapping))
    incrementalAnalysisElements.getOutgoing.artifact(
      compileScalaMapping,
      new Action[ConfigurablePublishArtifact]:
        override def execute(configurablePublishArtifact: ConfigurablePublishArtifact): Unit = configurablePublishArtifact.builtBy(compileScala)
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

  // Adopted from org.gradle.api.plugins.scala.ScalaPlugin.
  private def configureScaladoc(
    project: Project,
    isRegisterNotReplaceScaladocTask: Boolean,
    sourceSet: SourceSet,
    backendDisplayName: String,
    scalaDocTaskName: String
  ): Unit =
    project.getTasks.withType(classOf[ScalaDoc]).configureEach(scalaDoc =>
      // only for this backend
      if scalaDoc.getName == scalaDocTaskName then
        scalaDoc.getConventionMapping.map("classpath", () =>
          val files: ConfigurableFileCollection = project.files()
          files.from(sourceSet.getOutput)
          files.from(sourceSet.getCompileClasspath)
          files
        )
        scalaDoc.setSource(sourceSet.getExtensions.getByType(classOf[ScalaSourceDirectorySet]))
        scalaDoc.getCompilationOutputs.from(sourceSet.getOutput)
    )

    val scalaDocTaskConfigureAction: Action[Task] = (scalaDoc: Task) =>
      scalaDoc.setDescription(s"Generates Scaladoc for the $backendDisplayName main source code.")
      scalaDoc.setGroup(JavaBasePlugin.DOCUMENTATION_GROUP)

    if isRegisterNotReplaceScaladocTask then project.getTasks.register(
      scalaDocTaskName,
      classOf[ScalaDoc],
      scalaDocTaskConfigureAction
    ) else project.getTasks
      .named(scalaDocTaskName)
      .configure(scalaDocTaskConfigureAction)

// TODO - this is reported by `./gradlew resolvableConfigurations` even without me touching anything:
// Consumable configurations with identical capabilities within a project
// (other than the default configuration) must have unique attributes,
// but configuration ':incrementalScalaAnalysisFormain' and [configuration ':incrementalScalaAnalysisElements']
// contain identical attribute sets.
// Consider adding an additional attribute to one of the configurations to disambiguate them.
// For more information, please refer to
// https://docs.gradle.org/8.13/userguide/upgrading_version_7.html#unique_attribute_sets
// in the Gradle documentation.
