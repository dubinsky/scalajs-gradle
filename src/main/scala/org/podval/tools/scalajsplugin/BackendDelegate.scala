package org.podval.tools.scalajsplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.{ComponentIdentifier, ProjectComponentIdentifier}
import org.gradle.api.{Action, Project, Task}
import org.gradle.api.file.{DirectoryProperty, FileCollection, FileTreeElement, RegularFileProperty, SourceDirectorySet}
import org.gradle.api.internal.lambdas.SerializableLambdas
import org.gradle.api.internal.tasks.{DefaultSourceSet, DefaultSourceSetOutput}
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.internal.JvmPluginsHelper
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.{AbstractCompile, CompileOptions}
import org.gradle.api.tasks.scala.{IncrementalCompileOptions, ScalaCompile}
import org.gradle.api.tasks.{ScalaSourceDirectorySet, SourceSet, TaskProvider}
import org.gradle.internal.Cast
import org.gradle.internal.deprecation.DeprecationLogger
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.{JavaLauncher, JavaToolchainService}
import org.podval.tools.build.{DependencyRequirement, Gradle, ScalaPlatform}
import scala.jdk.CollectionConverters.*

object BackendDelegate:
  final val sharedSourceRoot: String = "shared"

abstract class BackendDelegate(
  project: Project,
  objectFactory: ObjectFactory,
):
  def sourceRoot: String

  def mainSourceSetName: String
  
  def testSourceSetName: String

  def setUpProject(): Unit

  def configurationToAddToClassPath: Option[String]

  def configureProject(projectScalaPlatform: ScalaPlatform): Unit

  def dependencyRequirements(
    pluginScalaPlatform: ScalaPlatform,
    projectScalaPlatform: ScalaPlatform
  ): Seq[DependencyRequirement]

  // -------------------------------------------------------------------------------------------
  // following code for setting up source sets, configurations and other Gradle things for Scala
  // was copied, translates and adjusted for jvm/js/shared split from the Gradle sources...
  // -------------------------------------------------------------------------------------------

  // To create a pair of platform-specific sourceSets, say `mainJS` and `testJS`,
  // we need to start all the way back from `ScalaBasePlugin.apply()` and `ScalaPlugin.apply()`,
  // and track into JavaPlugin...
  protected final def createBackendSpecificSetup(
    
  ) = ()
    
  // TODO when creating a Scala.js set of source sets, more from ScalaBasePlugin.apply():
  // configureConfigurations()
  // configureCompileDefaults()
  // configureScaladoc()
  
  // see org.gradle.api.plugins.scala.ScalaBasePlugin.configureSourceSetDefaults
  // we can not assume that there are only `test` and `main` sourceSets,
  // we need to name names:
  protected final def configureSourceSetDefaults(isCreate: Boolean): Unit =
    configureSourceSetDefaults(Gradle.getSourceSet(project, mainSourceSetName), isCreate)
    configureSourceSetDefaults(Gradle.getSourceSet(project, testSourceSetName), isCreate)

  protected final def configureSourceSetDefaults(
    sourceSet: SourceSet,
    isCreate: Boolean
  ): Unit =
    val scalaSource: ScalaSourceDirectorySet =
      if isCreate then
        val scalaSource: ScalaSourceDirectorySet = createScalaSourceDirectorySet(sourceSet)
        sourceSet.getExtensions.add(classOf[ScalaSourceDirectorySet], "scala", scalaSource)
        scalaSource
      else
        sourceSet.getExtensions.getByType(classOf[ScalaSourceDirectorySet])

    scalaSource.setSrcDirs(
      Seq(
        s"$sourceRoot/src/${sourceSet.getName}/scala",
        s"${BackendDelegate.sharedSourceRoot}/src/${sourceSet.getName}/scala"
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
      ()
    // TODO   project.getConfigurations.getByName(sourceSet.getImplementationConfigurationName).getDependencies.addLater(createScalaDependency(scalaPluginExtension))

    // TODO - this is reported by `./gradlew resolvableConfigurations` even without me touching anything:
    // Consumable configurations with identical capabilities within a project
    // (other than the default configuration) must have unique attributes,
    // but configuration ':incrementalScalaAnalysisFormain' and [configuration ':incrementalScalaAnalysisElements']
    // contain identical attribute sets.
    // Consider adding an additional attribute to one of the configurations to disambiguate them.
    // For more information, please refer to
    // https://docs.gradle.org/8.13/userguide/upgrading_version_7.html#unique_attribute_sets
    // in the Gradle documentation.

    val incrementalAnalysis: Configuration =
      if isCreate then
        ??? // TODO createIncrementalAnalysisConfigurationFor(project.getConfigurations, incrementalAnalysisCategory, incrementalAnalysisUsage, sourceSet)
      else
        project.getConfigurations.getByName(s"incrementalScalaAnalysisFor${sourceSet.getName}")

    createScalaCompileTask(
      isCreate,
      sourceSet,
      scalaSource,
      incrementalAnalysis
    )

  // see org.gradle.api.plugins.scala.ScalaBasePlugin.createScalaSourceDirectorySet
  /**
   * In 9.0, once {@link org.gradle.api.internal.tasks.DefaultScalaSourceSet} is removed, we can update this to only construct the source directory
   * set instead of the entire source set.
   */
  private def createScalaSourceDirectorySet(sourceSet: SourceSet): ScalaSourceDirectorySet =
    val scalaSourceSet = objectFactory.newInstance(
      classOf[org.gradle.api.internal.tasks.DefaultScalaSourceSet],
      sourceSet.asInstanceOf[DefaultSourceSet].getDisplayName,
      objectFactory
    )
    DeprecationLogger.whileDisabled(() =>
      DslObject(sourceSet).getConvention.getPlugins.put("scala", scalaSourceSet)
    )
    scalaSourceSet.getScala

  // see org.gradle.api.plugins.scala.ScalaBasePlugin.createScalaCompileTask
  private def createScalaCompileTask(
    isCreate: Boolean, // TODO
    sourceSet: SourceSet,
    scalaSource: ScalaSourceDirectorySet,
    incrementalAnalysis: Configuration
  ): Unit =
    val name: String = sourceSet.getCompileTaskName("scala") // TODO or scalaJS!

    val configure: Action[Task] = (task: Task) =>
      val scalaCompile: ScalaCompile = task.asInstanceOf[ScalaCompile]
      JvmPluginsHelper.compileAgainstJavaOutputs(scalaCompile, sourceSet, objectFactory)
      configureAnnotationProcessorPath(sourceSet, scalaSource, scalaCompile.getOptions)
      scalaCompile.setDescription(s"Compiles the $scalaSource.")
      scalaCompile.setSource(scalaSource)
      scalaCompile.getJavaLauncher.convention(getJavaLauncher)
      configureIncrementalAnalysis(sourceSet, incrementalAnalysis, scalaCompile)

    val compileTask: TaskProvider[? <: Task] =
      if isCreate then
        project.getTasks.register(name, classOf[ScalaCompile], configure)
      else
        val task: TaskProvider[Task] = project.getTasks.named(name)
        task.configure(configure)
        task

    configureOutputDirectoryForSourceSet(sourceSet, scalaSource, compileTask)
 
    if isCreate then
      val action: Action[Task] = _.dependsOn(compileTask)
      project.getTasks.named(sourceSet.getClassesTaskName, action)

  // see org.gradle.api.plugins.internal.JvmPluginsHelper.configureAnnotationProcessorPath
  private def configureAnnotationProcessorPath(
    sourceSet: SourceSet,
    sourceDirectorySet: SourceDirectorySet,
    options: CompileOptions,
  ): Unit =
    DslObject(options).getConventionMapping.map("annotationProcessorPath", () => sourceSet.getAnnotationProcessorPath)
    convention(options.getGeneratedSourceOutputDirectory, "generated/sources/annotationProcessor", sourceSet, sourceDirectorySet)

  // see org.gradle.api.plugins.scala.ScalaBasePlugin.getJavaLauncher
  private def getJavaLauncher: Provider[JavaLauncher] = project
    .getExtensions
    .getByType(classOf[JavaToolchainService])
    .launcherFor(project
      .getExtensions
      .getByType(classOf[JavaPluginExtension])
      .getToolchain
    )

  // see org.gradle.api.plugins.internal.JvmPluginsHelper.configureOutputDirectoryForSourceSet
  private def configureIncrementalAnalysis(
    sourceSet: SourceSet,
    incrementalAnalysis: Configuration,
    scalaCompile: ScalaCompile
  ): Unit =
    def set(property: RegularFileProperty, directory: String, extension: String): Unit =
      property.set(
        project.getLayout.getBuildDirectory.file(
          s"tmp/$sourceRoot/scala/$directory/${scalaCompile.getName}.$extension"
        )
      )

    set(scalaCompile.getAnalysisMappingFile, "compilerAnalysis", "mapping")

    // cannot compute at task execution time because we need association with source set
    val incrementalOptions: IncrementalCompileOptions = scalaCompile.getScalaCompileOptions.getIncrementalOptions
    set(incrementalOptions.getAnalysisFile, "compilerAnalysis", "analysis")
    set(incrementalOptions.getClassfileBackupDir, "classfileBackup", "bak")

    val jarTask: Jar = project.getTasks.findByName(sourceSet.getJarTaskName).asInstanceOf[Jar]
    if jarTask != null then
      incrementalOptions.getPublishedCode.set(jarTask.getArchiveFile)

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

  // see org.gradle.api.plugins.internal.JvmPluginsHelper.configureOutputDirectoryForSourceSet
  private def configureOutputDirectoryForSourceSet(
    sourceSet: SourceSet,
    sourceDirectorySet: SourceDirectorySet,
    compileTask: TaskProvider[? <: Task]
  ): Unit =
    convention(sourceDirectorySet.getDestinationDirectory, "classes", sourceSet, sourceDirectorySet)

    val sourceSetOutput: DefaultSourceSetOutput = Cast.cast(classOf[DefaultSourceSetOutput], sourceSet.getOutput)
    sourceSetOutput.getClassesDirs.from(sourceDirectorySet.getDestinationDirectory).builtBy(compileTask)
    sourceSetOutput.getGeneratedSourcesDirs.from(compileTask.map(_.asInstanceOf[ScalaCompile].getOptions).flatMap(_.getGeneratedSourceOutputDirectory))
    sourceDirectorySet.compiledBy(compileTask, _.asInstanceOf[AbstractCompile].getDestinationDirectory)

  private def convention(
    directoryProperty:  DirectoryProperty,
    path: String,
    sourceSet: SourceSet,
    sourceDirectorySet: SourceDirectorySet
  ): DirectoryProperty =
    directoryProperty.convention(
      project.getLayout.getBuildDirectory.dir(
        s"$sourceRoot/$path/${sourceDirectorySet.getName}/${sourceSet.getName}"
      )
    )
