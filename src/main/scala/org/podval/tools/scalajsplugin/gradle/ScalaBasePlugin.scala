package org.podval.tools.scalajsplugin.gradle

import com.google.common.base.{Joiner, Splitter}
import com.google.common.collect.ImmutableSet
import org.gradle.api.{Action, ActionConfiguration, InvalidUserCodeException, JavaVersion, Plugin, Project, Task}
import org.gradle.api.artifacts.{Configuration, Dependency, DependencyArtifact, DependencyConstraint,
  DependencyScopeConfiguration, ModuleDependency, ResolvableConfiguration, ResolvableDependencies}
import org.gradle.api.artifacts.component.{ComponentIdentifier, ProjectComponentIdentifier}
import org.gradle.api.artifacts.dsl.{DependencyFactory, DependencyHandler}
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.attributes.{AttributeDisambiguationRule, AttributeMatchingStrategy, Category,
  MultipleCandidatesDetails, Usage}
import org.gradle.api.file.{FileCollection, FileTreeElement, RegularFileProperty}
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.artifacts.configurations.{ConfigurationRolesForMigration,
  RoleBasedConfigurationContainerInternal}
import org.gradle.api.internal.lambdas.SerializableLambdas
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.provider.Providers
import org.gradle.api.internal.tasks.DefaultSourceSet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.{ExtensionAware, JavaBasePlugin, JavaPluginExtension}
import org.gradle.api.plugins.internal.DefaultJavaPluginExtension
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.plugins.scala.ScalaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.{ScalaRuntime, ScalaSourceDirectorySet, SourceSet, TaskProvider}
import org.gradle.api.tasks.scala.{IncrementalCompileOptions, ScalaCompile, ScalaDoc}
import org.gradle.api.tasks.scala.internal.ScalaRuntimeHelper
import org.gradle.internal.deprecation.DeprecationLogger
import org.gradle.internal.logging.util.Log4jBannedVersion
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.{JavaLauncher, JavaToolchainService}
import org.gradle.language.scala.tasks.{AbstractScalaCompile, KeepAliveMode}
import org.podval.tools.build.Gradle
import org.podval.tools.scalajsplugin.GradleNames
import scala.jdk.CollectionConverters.*
import javax.inject.Inject
import java.io.File
import java.util.concurrent.Callable

// TODO - this is reported by `./gradlew resolvableConfigurations` even without me touching anything:
// Consumable configurations with identical capabilities within a project
// (other than the default configuration) must have unique attributes,
// but configuration ':incrementalScalaAnalysisFormain' and [configuration ':incrementalScalaAnalysisElements']
// contain identical attribute sets.
// Consider adding an additional attribute to one of the configurations to disambiguate them.
// For more information, please refer to
// https://docs.gradle.org/8.13/userguide/upgrading_version_7.html#unique_attribute_sets
// in the Gradle documentation.

// see org.gradle.api.plugins.scala.ScalaBasePlugin
// changes:
// - make it work directly, not as a part of plugin application (which already happened);
// - supports both creation of new sourceSets and configuring the existing ones;
// - do not assume that there is only one pair of source sets (`main` and `test`);
// - added `project` parameter;
// - added `isCreate` parameter;
// - added `sourceRoot` parameter;
// - added `sharedSourceRoot` parameter;
// - added `gradleNames` parameter;
import org.gradle.api.plugins.scala.ScalaBasePlugin as Original

final class ScalaBasePlugin(
  isCreate: Boolean,
  sourceRoot: String,
  sharedSourceRoot: String,
  gradleNames: GradleNames,
  project: Project,
  jvmPluginServices: JvmPluginServices // TODO get from the project?
):
  import ScalaBasePlugin.{javaPluginExtension, getJavaLauncher}

  private def objectFactory: ObjectFactory = project.getObjects
  private def dependencyFactory: DependencyFactory = project.getDependencyFactory

  def apply(): Unit =
    // TODO if isCreate then new JavaBasePlugin(...).apply()

    // Scala Runtime Extension, Scala Plugin Extension and Scala Toolchain already exist;
    // they are shared, so we do NOT create a new ones :)
    
    // project.getExtensions.create(Original.SCALA_RUNTIME_EXTENSION_NAME, classOf[ScalaRuntime], project)
    val scalaRuntime: ScalaRuntime = project.getExtensions.getByType(classOf[ScalaRuntime])
    // project.getExtensions.create("scala", classOf[ScalaPluginExtension])
    val scalaPluginExtension: ScalaPluginExtension = project.getExtensions.getByType(classOf[ScalaPluginExtension])
    // scalaPluginExtension.getZincVersion.convention(Original.DEFAULT_ZINC_VERSION)

    //createToolchainRuntimeClasspath(scalaPluginExtension)
    val toolchainClasspath: Provider[ResolvableConfiguration] = Providers.of(
      project.getConfigurations.getByName("scalaToolchainRuntimeClasspath").asInstanceOf[ResolvableConfiguration]
    )

    // TODO retrieve those or make new ones?
    val incrementalAnalysisUsage: Usage = objectFactory.named(classOf[Usage], "incremental-analysis")
    val incrementalAnalysisCategory: Category = objectFactory.named(classOf[Category], "scala-analysis")

    if isCreate then configureCompilerPlugins(project.asInstanceOf[ProjectInternal])
//      configureConfigurations(
//        project.asInstanceOf[ProjectInternal],
//      //  incrementalAnalysisCategory,
//      //  incrementalAnalysisUsage,
//      //  scalaPluginExtension
//      )

      configureCompileDefaults(
        project,
        scalaRuntime,
        javaPluginExtension(project).asInstanceOf[DefaultJavaPluginExtension],
        scalaPluginExtension,
        toolchainClasspath
      )

    configureSourceSetDefaults(
      project.asInstanceOf[ProjectInternal],
      incrementalAnalysisCategory,
      incrementalAnalysisUsage,
      scalaPluginExtension
    )

    if isCreate then // TODO do we need to reconfigure anything for the sourceRoot even when !isCreate?
      ScalaBasePlugin.configureScaladoc(
        project,
        scalaRuntime,
        scalaPluginExtension,
        toolchainClasspath
      )
  
  // extracted from configureConfigurations()
  private def configureCompilerPlugins(
    project: ProjectInternal
  ): Unit =
    val plugins: Configuration = project.getConfigurations.resolvableDependencyScopeUnlocked(
      gradleNames.compilerPluginsConfigurationName
    )
    plugins.setTransitive(false)
    jvmPluginServices.configureAsRuntimeClasspath(plugins)
    
  private def configureSourceSetDefaults(
    project: ProjectInternal,
    incrementalAnalysisCategory: Category,
    incrementalAnalysisUsage: Usage,
    scalaPluginExtension: ScalaPluginExtension
  ): Unit =
    // we can not assume that there are only `test` and `main` sourceSets,
    // we need to name names, so instead od
    //   javaPluginExtension(project).getSourceSets.all()
    // we do:
    Seq(
      gradleNames.mainSourceSetName,
      gradleNames.testSourceSetName
    )
      .map(Gradle.getSourceSet(project, _))
      .foreach((sourceSet: SourceSet) =>
        configureSourceSetDefaults(
          project,
          incrementalAnalysisCategory,
          incrementalAnalysisUsage,
          scalaPluginExtension,
          sourceSet
        )
      )

  private def configureSourceSetDefaults(
    project: ProjectInternal,
    incrementalAnalysisCategory: Category,
    incrementalAnalysisUsage: Usage,
    scalaPluginExtension: ScalaPluginExtension,
    sourceSet: SourceSet
  ): Unit =
    val scalaSource: ScalaSourceDirectorySet =
      if !isCreate
      then sourceSet.getExtensions.getByType(
        classOf[ScalaSourceDirectorySet]
      )
      else
        val scalaSource: ScalaSourceDirectorySet = createScalaSourceDirectorySet(sourceSet)
        sourceSet.getExtensions.add(
          classOf[ScalaSourceDirectorySet],
          "scala", 
          scalaSource
        )
        scalaSource

    scalaSource.setSrcDirs(
      Seq(
        s"$sourceRoot/src/${sourceSet.getName}/scala",
        s"$sharedSourceRoot/src/${sourceSet.getName}/scala"
      )
        .map(project.file)
        .asJava
    )

    // Explicitly capture only a FileCollection in the lambda below for compatibility with configuration-cache.
    val scalaSourceFiles: FileCollection = scalaSource
    sourceSet.getResources.getFilter.exclude(
      SerializableLambdas.spec(
        (element: FileTreeElement) => scalaSourceFiles.contains(element.getFile)
      )
    )

    sourceSet.getAllJava.source(scalaSource)
    sourceSet.getAllSource.source(scalaSource)

    if isCreate then
      project
        .getConfigurations
        .getByName(sourceSet.getImplementationConfigurationName)
        .getDependencies
        .addLater(createScalaDependency(scalaPluginExtension))

    val incrementalAnalysis: Configuration = createIncrementalAnalysisConfigurationFor(
      project.getConfigurations,
      incrementalAnalysisCategory,
      incrementalAnalysisUsage,
      sourceSet
    )

    createScalaCompileTask(
      sourceSet,
      scalaSource,
      incrementalAnalysis
    )

  private def createScalaDependency(scalaPluginExtension: ScalaPluginExtension): Provider[Dependency] =
    scalaPluginExtension.getScalaVersion.map(scalaVersion =>
      if ScalaRuntimeHelper.isScala3(scalaVersion)
      then dependencyFactory.create("org.scala-lang", "scala3-library_3", scalaVersion)
      else dependencyFactory.create("org.scala-lang", "scala-library", scalaVersion)
    )
  
  /**
   * In 9.0, once org.gradle.api.internal.tasks.DefaultScalaSourceSet is removed, we can update this to only construct the source directory
   * set instead of the entire source set.
   */
  private def createScalaSourceDirectorySet(sourceSet: SourceSet): ScalaSourceDirectorySet =
    val scalaSourceSet: org.gradle.api.internal.tasks.DefaultScalaSourceSet = objectFactory.newInstance(
      classOf[org.gradle.api.internal.tasks.DefaultScalaSourceSet],
      sourceSet.asInstanceOf[DefaultSourceSet].getDisplayName,
      objectFactory
    )
    DeprecationLogger.whileDisabled(() =>
      DslObject(sourceSet).getConvention.getPlugins.put("scala", scalaSourceSet)
    )
    scalaSourceSet.getScala

  private def createScalaCompileTask(
    sourceSet: SourceSet,
    scalaSource: ScalaSourceDirectorySet,
    incrementalAnalysis: Configuration
  ): Unit =
    val name: String = sourceSet.getCompileTaskName(gradleNames.scalaCompileTaskName)

    val configurationAction: Action[Task] = (task: Task) =>
      val scalaCompile: ScalaCompile = task.asInstanceOf[ScalaCompile]
      JvmPluginsHelper.compileAgainstJavaOutputs(scalaCompile, sourceSet, objectFactory)

      JvmPluginsHelper.configureAnnotationProcessorPath(
        project,
        sourceRoot,
        sourceSet,
        scalaSource,
        scalaCompile.getOptions
      )

      scalaCompile.setDescription(s"Compiles the $scalaSource.")
      scalaCompile.setSource(scalaSource)
      scalaCompile.getJavaLauncher.convention(getJavaLauncher(project))
      configureIncrementalAnalysis(sourceSet, incrementalAnalysis, scalaCompile)

    val compileTask: TaskProvider[? <: Task] =
      if isCreate then
        project.getTasks.register(name, classOf[ScalaCompile], configurationAction)
      else
        val task: TaskProvider[Task] = project.getTasks.named(name)
        task.configure(configurationAction)
        task

    JvmPluginsHelper.configureOutputDirectoryForSourceSet(
      project,
      sourceRoot,
      sourceSet,
      scalaSource,
      compileTask
    )

    if isCreate then
      project.getTasks.named(sourceSet.getClassesTaskName,
        new Action[Task]:
          override def execute(task: Task): Unit = task.dependsOn(compileTask)
      )

  private def configureIncrementalAnalysis(
    sourceSet: SourceSet,
    incrementalAnalysis: Configuration,
    scalaCompile: ScalaCompile
  ): Unit =
    // to clean up code duplication
    def set(property: RegularFileProperty, directory: String, extension: String): Unit =
      property.set(
        project.getLayout.getBuildDirectory.file(
          s"tmp/scala/$directory/${scalaCompile.getName}.$extension"
        )
      )

    set(scalaCompile.getAnalysisMappingFile, "compilerAnalysis", "mapping")

    // cannot compute at task execution time because we need association with source set
    val incrementalOptions: IncrementalCompileOptions = scalaCompile.getScalaCompileOptions.getIncrementalOptions
    set(incrementalOptions.getAnalysisFile, "compilerAnalysis", "analysis")
    set(incrementalOptions.getClassfileBackupDir, "classfileBackup", "bak")

    val jarTask: Jar = project.getTasks.findByName(sourceSet.getJarTaskName).asInstanceOf[Jar]
    if jarTask != null then incrementalOptions.getPublishedCode.set(jarTask.getArchiveFile)

    scalaCompile.getAnalysisFiles.from(incrementalAnalysis.getIncoming.artifactView(viewConfiguration =>
      viewConfiguration.lenient(true)
      viewConfiguration.componentFilter(
        SerializableLambdas.spec(
          (element: ComponentIdentifier) => element.isInstanceOf[ProjectComponentIdentifier]
        )
      )
    ).getFiles)

    // See https://github.com/gradle/gradle/issues/14434.  We do this so that the incrementalScalaAnalysisForXXX configuration
    // is resolved during task graph calculation.  It is not an input, but if we leave it to be resolved during task execution,
    // it can potentially block trying to resolve project dependencies.
    scalaCompile.dependsOn(scalaCompile.getAnalysisFiles)

  private def createIncrementalAnalysisConfigurationFor(
    configurations: RoleBasedConfigurationContainerInternal,
    incrementalAnalysisCategory: Category,
    incrementalAnalysisUsage: Usage,
    sourceSet: SourceSet
  ): Configuration =
    val name: String = s"incrementalScalaAnalysisFor${sourceSet.getName}"
    if !isCreate
    then configurations.getByName(name)
    else
      val classpath: Configuration = configurations.getByName(sourceSet.getImplementationConfigurationName)
      val incrementalAnalysis: Configuration = configurations.migratingUnlocked(
        name,
        ConfigurationRolesForMigration.RESOLVABLE_DEPENDENCY_SCOPE_TO_RESOLVABLE
      )
      incrementalAnalysis.setVisible(false)
      incrementalAnalysis.setDescription(s"Incremental compilation analysis files for ${sourceSet.asInstanceOf[DefaultSourceSet].getDisplayName}")
      incrementalAnalysis.extendsFrom(classpath)
      incrementalAnalysis.getAttributes.attribute(Usage.USAGE_ATTRIBUTE, incrementalAnalysisUsage)
      incrementalAnalysis.getAttributes.attribute(Category.CATEGORY_ATTRIBUTE, incrementalAnalysisCategory)
      incrementalAnalysis

  private def configureCompileDefaults(
    project: Project,
    scalaRuntime: ScalaRuntime,
    javaExtension: DefaultJavaPluginExtension,
    scalaPluginExtension: ScalaPluginExtension,
    scalaToolchainRuntimeClasspath: Provider[ResolvableConfiguration]
  ): Unit =
    // TODO do NOT configure all ScalaCompile tasks - get only the ones that belong to the source sets we are operating on!
    project.getTasks.withType(classOf[ScalaCompile]).configureEach(compile =>
      val conventionMapping: ConventionMapping = compile.getConventionMapping
      conventionMapping.map("scalaClasspath", () => ScalaBasePlugin.getScalaToolchainClasspath(
        scalaPluginExtension,
        scalaToolchainRuntimeClasspath,
        scalaRuntime,
        compile.getClasspath
      ))
      conventionMapping.map("zincClasspath", () => project.getConfigurations.getAt(Original.ZINC_CONFIGURATION_NAME))
      conventionMapping.map("scalaCompilerPlugins", () => project.getConfigurations.getAt(gradleNames.compilerPluginsConfigurationName))
      conventionMapping.map("sourceCompatibility", () => ScalaBasePlugin.computeJavaSourceCompatibilityConvention(javaExtension, compile).toString)
      conventionMapping.map("targetCompatibility", () => ScalaBasePlugin.computeJavaTargetCompatibilityConvention(javaExtension, compile).toString)
      compile.getScalaCompileOptions.getKeepAliveMode.convention(KeepAliveMode.SESSION)
    )

object ScalaBasePlugin:
  private def computeJavaSourceCompatibilityConvention(javaExtension: DefaultJavaPluginExtension, compileTask: ScalaCompile): JavaVersion =
    val rawSourceCompatibility: JavaVersion = javaExtension.getRawSourceCompatibility
    if rawSourceCompatibility != null
    then rawSourceCompatibility
    else JavaVersion.toVersion(compileTask.getJavaLauncher.get.getMetadata.getLanguageVersion.toString)

  private def computeJavaTargetCompatibilityConvention(javaExtension: DefaultJavaPluginExtension, compileTask: ScalaCompile): JavaVersion =
    val rawTargetCompatibility: JavaVersion = javaExtension.getRawTargetCompatibility
    if rawTargetCompatibility != null
    then rawTargetCompatibility
    else JavaVersion.toVersion(compileTask.getSourceCompatibility)

  private def configureScaladoc(
     project: Project,
     scalaRuntime: ScalaRuntime,
     scalaPluginExtension: ScalaPluginExtension,
     scalaToolchainRuntimeClasspath: Provider[ResolvableConfiguration]
  ): Unit =
    project.getTasks.withType(classOf[ScalaDoc]).configureEach(scalaDoc =>
      scalaDoc.getConventionMapping.map("scalaClasspath", () => getScalaToolchainClasspath(
        scalaPluginExtension,
        scalaToolchainRuntimeClasspath,
        scalaRuntime,
        scalaDoc.getClasspath
      ))
      scalaDoc.getConventionMapping.map("destinationDir", () => javaPluginExtension(project).getDocsDir.dir("scaladoc").get().getAsFile)
      scalaDoc.getConventionMapping.map("title", () => project.getExtensions.getByType(classOf[ReportingExtension]).getApiDocTitle)
      scalaDoc.getJavaLauncher.convention(getJavaLauncher(project))
    )

  private def getScalaToolchainClasspath(
    scalaPluginExtension: ScalaPluginExtension,
    scalaToolchainRuntimeClasspath: Provider[ResolvableConfiguration],
    scalaRuntime: ScalaRuntime,
    taskClasspath: FileCollection
  ): FileCollection =
    if scalaPluginExtension.getScalaVersion.isPresent
    then scalaToolchainRuntimeClasspath.get
    else
      // TODO: Deprecate this path in 9.x when we de-incubate ScalaPluginExtension#getScalaVersion()
      scalaRuntime.inferScalaClasspath(taskClasspath)

  private def getJavaLauncher(project: Project): Provider[JavaLauncher] =
    extensionOf(project, classOf[JavaToolchainService])
      .launcherFor(javaPluginExtension(project).getToolchain)

  private def javaPluginExtension(project: Project): JavaPluginExtension =
    extensionOf(project, classOf[JavaPluginExtension])

  private def extensionOf[T](extensionAware: ExtensionAware, typ: Class[T]): T =
    extensionAware.getExtensions.getByType(typ)

// private val DEFAULT_SCALA_ZINC_VERSION: String = "2.13"

//  class UsageDisambiguationRules @Inject(
//      incrementalAnalysis: Usage,
//      javaApi: Usage,
//      javaRuntime: Usage
//    ) extends AttributeDisambiguationRule[Usage]:
//      private val expectedUsages: ImmutableSet[Usage] = ImmutableSet.of(
//        incrementalAnalysis,
//        javaApi,
//        javaRuntime
//      )
//
//      override def execute(details: MultipleCandidatesDetails[Usage]): Unit =
//        if details.getConsumerValue == null then
//          if details.getCandidateValues.equals(expectedUsages) then
//            details.closestMatch(javaRuntime)

//  private def configureConfigurations(
//    project: ProjectInternal,
//    // incrementalAnalysisCategory: Category,
//    // incrementalAnalysisUsage: Usage,
//    // scalaPluginExtension: ScalaPluginExtension
//  ): Unit =
////    val dependencyHandler: DependencyHandler = project.getDependencies
//    configureCompilerPlugins(project)
//
//    // Zinc and incrementalAnalysisElements setups already exist and are shared, so we do NOT create new ones :)
//    // configureZinc(project, dependencyHandler, scalaPluginExtension)
//    // configureIncrementalAnalysisElements(project, incrementalAnalysisCategory, incrementalAnalysisUsage)

  // extracted from configureConfigurations()
//  private def configureIncrementalAnalysisElements(
//    project: ProjectInternal,
//    incrementalAnalysisCategory: Category,
//    incrementalAnalysisUsage: Usage,
//  ) =
//    val incrementalAnalysisElements: Configuration = project.getConfigurations.migratingUnlocked(
//      "incrementalScalaAnalysisElements",
//      ConfigurationRolesForMigration.CONSUMABLE_DEPENDENCY_SCOPE_TO_CONSUMABLE
//    )
//    incrementalAnalysisElements.setVisible(false)
//    incrementalAnalysisElements.setDescription("Incremental compilation analysis files")
//    incrementalAnalysisElements.getAttributes.attribute(Usage.USAGE_ATTRIBUTE, incrementalAnalysisUsage)
//    incrementalAnalysisElements.getAttributes.attribute(Category.CATEGORY_ATTRIBUTE, incrementalAnalysisCategory)
//    
//    val matchingStrategy: AttributeMatchingStrategy[Usage] = dependencyHandler.getAttributesSchema.attribute(Usage.USAGE_ATTRIBUTE)
//    matchingStrategy.getDisambiguationRules.add(
//      classOf[ScalaBasePlugin.UsageDisambiguationRules],
//      new Action[ActionConfiguration]:
//        override def execute(actionConfiguration: ActionConfiguration): Unit =
//          actionConfiguration.params(incrementalAnalysisUsage)
//          actionConfiguration.params(objectFactory.named(classOf[Usage], Usage.JAVA_API))
//          actionConfiguration.params(objectFactory.named(classOf[Usage], Usage.JAVA_RUNTIME))
//    )
    
// extracted from configureConfigurations()
//  private def configureZinc(
//    project: ProjectInternal,
//    dependencyHandler: DependencyHandler,
//    scalaPluginExtension: ScalaPluginExtension
//  ) =
//    val zinc: Configuration = project.getConfigurations.resolvableDependencyScopeUnlocked(Original.ZINC_CONFIGURATION_NAME)
//    zinc.setVisible(false)
//    zinc.setDescription("The Zinc incremental compiler to be used for this Scala project.")
//
//    zinc.getResolutionStrategy.eachDependency(rule =>
//      if rule.getRequested.getGroup.equals("com.typesafe.zinc") && rule.getRequested.getName.equals("zinc") then
//        rule.useTarget(s"org.scala-sbt:zinc_${ScalaBasePlugin.DEFAULT_SCALA_ZINC_VERSION}:${Original.DEFAULT_ZINC_VERSION}")
//        rule.because("Typesafe Zinc is no longer maintained.")
//    )
//
//    zinc.defaultDependencies(dependencies =>
//      dependencies.add(dependencyHandler.create(s"org.scala-sbt:zinc_${ScalaBasePlugin.DEFAULT_SCALA_ZINC_VERSION}:${scalaPluginExtension.getZincVersion.get}"))
//      // Add safeguard and clear error if the user changed the scala version when using default zinc
//      zinc.getIncoming.afterResolve(
//        new Action[ResolvableDependencies]:
//          override def execute(resolvableDependencies: ResolvableDependencies): Unit =
//            resolvableDependencies.getResolutionResult.allComponents(
//              new Action[ResolvedComponentResult]:
//                override def execute(component: ResolvedComponentResult): Unit =
//                  if component.getModuleVersion != null && component.getModuleVersion.getName.equals("scala-library") then
//                    if !component.getModuleVersion.getVersion.startsWith(ScalaBasePlugin.DEFAULT_SCALA_ZINC_VERSION) then
//                      throw new InvalidUserCodeException("The version of 'scala-library' was changed while using the default Zinc version. " +
//                        s"Version ${component.getModuleVersion.getVersion} is not compatible with org.scala-sbt:zinc_${ScalaBasePlugin.DEFAULT_SCALA_ZINC_VERSION}:${Original.DEFAULT_ZINC_VERSION}")
//            )
//      )
//    )
//
//    zinc.getDependencyConstraints.add(dependencyHandler.getConstraints.create(
//      Log4jBannedVersion.LOG4J2_CORE_COORDINATES,
//      new Action[DependencyConstraint]:
//        override def execute(constraint: DependencyConstraint): Unit = constraint.version(version =>
//          version.require(Log4jBannedVersion.LOG4J2_CORE_REQUIRED_VERSION)
//          version.reject(Log4jBannedVersion.LOG4J2_CORE_VULNERABLE_VERSION_RANGE)
//        )
//    ))

  // Scala Toolchain setup is shared, so we do NOT create a second one :)
//  private def createToolchainRuntimeClasspath(
//    scalaPluginExtension: ScalaPluginExtension
//  ): Provider[ResolvableConfiguration] =
//    val scalaToolchain: Provider[DependencyScopeConfiguration] = project.getConfigurations.dependencyScope(
//      "scalaToolchain",
//      (conf: DependencyScopeConfiguration) =>
//        conf.setDescription("Dependencies for the Scala toolchain")
//        conf.getDependencies.addLater(createScalaCompilerDependency(scalaPluginExtension))
//        conf.getDependencies.addLater(createScalaBridgeDependency(scalaPluginExtension))
//        conf.getDependencies.addLater(createScalaCompilerInterfaceDependency(scalaPluginExtension))
//        conf.getDependencies.addLater(createScaladocDependency(scalaPluginExtension))
//    )
//
//    project.getConfigurations.resolvable(
//      "scalaToolchainRuntimeClasspath",
//      (conf: ResolvableConfiguration) =>
//        conf.setDescription("Runtime classpath for the Scala toolchain")
//        conf.extendsFrom(scalaToolchain.get())
//        jvmPluginServices.configureAsRuntimeClasspath(conf)
//    )

//  private def createScalaCompilerDependency(scalaPluginExtension: ScalaPluginExtension): Provider[Dependency] =
//    scalaPluginExtension.getScalaVersion.map(scalaVersion =>
//      if ScalaRuntimeHelper.isScala3(scalaVersion)
//      then dependencyFactory.create("org.scala-lang", "scala3-compiler_3", scalaVersion)
//      else dependencyFactory.create("org.scala-lang", "scala-compiler", scalaVersion)
//    )
//
//  private def createScalaBridgeDependency(scalaPluginExtension: ScalaPluginExtension): Provider[Dependency] =
//    scalaPluginExtension.getScalaVersion.zip(scalaPluginExtension.getZincVersion, (scalaVersion, zincVersion) =>
//      if ScalaRuntimeHelper.isScala3(scalaVersion)
//      then dependencyFactory.create("org.scala-lang", "scala3-sbt-bridge", scalaVersion)
//      else
//        val scalaMajorMinorVersion: String = Joiner.on('.').join(Splitter.on('.').splitToList(scalaVersion).subList(0, 2))
//        val name: String = s"compiler-bridge_$scalaMajorMinorVersion"
//        val dependency: ModuleDependency = dependencyFactory.create("org.scala-sbt", name, zincVersion)
//
//        // Use an artifact to remain compatible with Ivy repositories, which
//        // don't support variant derivation.
//        dependency.artifact((artifact: DependencyArtifact) =>
//          artifact.setClassifier("sources")
//          artifact.setType("jar")
//          artifact.setExtension("jar")
//          artifact.setName(name)
//        )
//
//        dependency
//    )
//
//  private def createScalaCompilerInterfaceDependency(scalaPluginExtension: ScalaPluginExtension): Provider[Dependency] =
//    scalaPluginExtension.getScalaVersion.zip(scalaPluginExtension.getZincVersion, (scalaVersion, zincVersion) =>
//      if ScalaRuntimeHelper.isScala3(scalaVersion)
//      then dependencyFactory.create("org.scala-lang", "scala3-interfaces", scalaVersion)
//      else dependencyFactory.create("org.scala-sbt", "compiler-interface", zincVersion)
//    )
//
//  private def createScaladocDependency(scalaPluginExtension: ScalaPluginExtension): Provider[Dependency] =
//    scalaPluginExtension.getScalaVersion.map(scalaVersion =>
//      if ScalaRuntimeHelper.isScala3(scalaVersion)
//      then dependencyFactory.create("org.scala-lang", "scaladoc_3", scalaVersion)
//      else null
//    )
