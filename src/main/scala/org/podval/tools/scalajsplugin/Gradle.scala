package org.podval.tools.scalajsplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.provider.ProviderInternal
import org.gradle.api.internal.tasks.JvmConstants
import org.gradle.api.plugins.{JavaPluginExtension, JvmTestSuitePlugin}
import org.gradle.api.{Action, Project, Task}
import org.gradle.api.plugins.jvm.internal.JvmFeatureInternal
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.{ScalaSourceDirectorySet, SourceSet, SourceSetContainer, TaskProvider}
import org.gradle.jvm.tasks.Jar
import org.gradle.util.internal.GUtil
import org.podval.tools.build.{ScalaBackend, ScalaLibrary}
import org.podval.tools.util.Files
import org.slf4j.{Logger, LoggerFactory}
import scala.jdk.CollectionConverters.*
import java.io.File
import java.lang.reflect.Field

object Gradle:
  private val logger: Logger = LoggerFactory.getLogger(classOf[BackendDelegate[?]])

  def getClassesTaskProvider(project: Project, sourceSet: SourceSet): TaskProvider[Task] = project
    .getTasks
    .named(sourceSet.getClassesTaskName)

  def findDependsOnProviderOrTask[T <: Task](task: Task, clazz: Class[? <: T]): Option[T] =
    findDependsOnTaskProvider(task, clazz)
      .map(_.get)
      .orElse(findDependsOnTask(task, clazz))

  private def findDependsOnTaskProvider[T <: Task](task: Task, clazz: Class[? <: T]): Option[TaskProvider[T]] = task
    .getDependsOn
    .asScala
    .filter(_.isInstanceOf[TaskProvider[?]])
    .filter(_.isInstanceOf[ProviderInternal[?]])
    .map(_.asInstanceOf[ProviderInternal[T]])
    .find(candidate => clazz.isAssignableFrom(candidate.getType))
    .map(_.asInstanceOf[TaskProvider[T]])

  private def findDependsOnTask[T <: Task](task: Task, clazz: Class[? <: T]): Option[T] = task
    .getDependsOn
    .asScala
    .filter(_.isInstanceOf[Task])
    .map(_.asInstanceOf[Task])
    .find(candidate => clazz.isAssignableFrom(candidate.getClass))
    .map(_.asInstanceOf[T])

  // TODO use task name!
  def scalaCompile(project: Project, sourceSet: SourceSet): ScalaCompile = Gradle
    .getClassesTaskProvider(project, sourceSet)
    .get
    .getDependsOn
    .asScala
    .find(classOf[TaskProvider[ScalaCompile]].isInstance)
    .get
    .asInstanceOf[TaskProvider[ScalaCompile]]
    .get

  def getSourceSets(project: Project): (SourceSet, SourceSet) = 
    val sourceSets: SourceSetContainer = project.getExtensions.getByType(classOf[JavaPluginExtension]).getSourceSets
    (
      sourceSets.getByName(JvmConstants      .JAVA_MAIN_FEATURE_NAME ),
      sourceSets.getByName(JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME)
    )

  def findSubproject(project: Project, name: String): Option[Project] = project
    .getSubprojects
    .asScala
    .find(_.getName == name)

  def disableAllTasks(project: Project): Unit = project
    .getTasks
    .withType(classOf[Task])
    .configureEach(_.setEnabled(false))

  private def getScalaSourceDirectorySet(sourceSet: SourceSet): ScalaSourceDirectorySet = sourceSet
    .getExtensions
    .getByType(classOf[ScalaSourceDirectorySet])  
  
  private def addScalaSources(sourceSet: SourceSet, sourceDirectory: File): Unit =
    getScalaSourceDirectorySet(sourceSet).srcDir(sourceDirectory)

  private val defaultSourceDirectorySetSource: Field = classOf[DefaultSourceDirectorySet].getDeclaredField("source")
  defaultSourceDirectorySetSource.setAccessible(true)
  
  private def removeScalaSources(sourceSet: SourceSet, sourceDirectory: File): Unit =
    val scalaSourceDirectorySet: ScalaSourceDirectorySet = getScalaSourceDirectorySet(sourceSet)
    val source: List[Object] = defaultSourceDirectorySetSource
      .get(scalaSourceDirectorySet)
      .asInstanceOf[java.util.List[java.lang.Object]]
      .asScala
      .toList
    val sourceFiltered: List[Object] = source.filterNot(o =>
      o.isInstanceOf[File] && o.asInstanceOf[File].getAbsolutePath == sourceDirectory.getAbsolutePath
    )
    scalaSourceDirectorySet.setSrcDirs(sourceFiltered.asJava)

  private def sharedScalaSources(project: Project, isTest: Boolean) = Files.file(
    project.getProjectDir.getParentFile, // mixed project
    ScalaBackend.sharedSourceRoot,       // shared sibling directory
    "src",
    if isTest then "test" else "main",
    "scala"
  )

  def addSharedScalaSources(project: Project): Unit =
    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(project)
    addScalaSources(mainSourceSet, sharedScalaSources(project, isTest = false))
    addScalaSources(testSourceSet, sharedScalaSources(project, isTest = true ))

  // Add before compilation and remove after, so that IntelliJ does not
  // run into "duplicate content roots" issue during project import.
  def addSharedScalaSourcesForTask(project: Project): Action[Task] = (task: Task) =>
    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(project)
    task.doFirst(new Action[Task]:
      override def execute(t: Task): Unit =
        addScalaSources(mainSourceSet, sharedScalaSources(project, isTest = false))
        addScalaSources(testSourceSet, sharedScalaSources(project, isTest = true ))
    )
    task.doLast(new Action[Task]:
      override def execute(t: Task): Unit =
        removeScalaSources(mainSourceSet, sharedScalaSources(project, isTest = false))
        removeScalaSources(testSourceSet, sharedScalaSources(project, isTest = true ))
    )

  private def removeAllScalaSources(sourceSet: SourceSet): Unit =
    getScalaSourceDirectorySet(sourceSet).setSrcDirs(List.empty.asJava)

  def removeAllScalaSources(project: Project): Unit =   
    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(project)
    removeAllScalaSources(mainSourceSet)
    removeAllScalaSources(testSourceSet)
  
  def archiveAppendixConvention(appendix: String, project: Project): Action[Jar] = (jar: Jar) => jar
    .getArchiveAppendix
    .convention(project.provider(() =>
      if jar.getArchiveClassifier.isPresent then appendix else null
    ))

  def removeDashBeforeArchiveAppendix(project: Project): Action[Jar] = (jar: Jar) => jar
    .getArchiveFileName
    .convention(project.provider(() =>
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

  def ensureParameters(
    scalaCompile: ScalaCompile,
    toAdd: Seq[String]
  ): Unit =
    val parameters: List[String] = Option(scalaCompile.getScalaCompileOptions.getAdditionalParameters) // nullable
      .map(_.asScala.toList)
      .getOrElse(List.empty)

    val parametersNew: List[String] = toAdd.foldLeft(parameters) {
      case (parameters, parameter) =>
        if parameters.contains(parameter) then parameters else
          logger.info(s"scalaCompileOptions.additionalParameters of the ${scalaCompile.getName} task: adding '$parameter'.")
          parameters :+ parameter
    }

    scalaCompile
      .getScalaCompileOptions
      .setAdditionalParameters(parametersNew.asJava)

  def addScalaCompilerPlugins(
    scalaCompilerPluginsConfiguration: Configuration,
    scalaCompile: ScalaCompile
  ): Unit =
    // There seems to be no need to add `"-Xplugin:" + plugin.getPath` parameters:
    // just adding plugins to the list is sufficient.
    val scalaCompilerPlugins: Iterable[File] = scalaCompilerPluginsConfiguration.asScala
    if scalaCompilerPlugins.nonEmpty then
      logger.info(s"Adding ${scalaCompile.getName} to ${scalaCompilerPluginsConfiguration.getName}: $scalaCompilerPlugins.")
      val plugins: FileCollection = Option(scalaCompile.getScalaCompilerPlugins)
        .map((existingPlugins: FileCollection) => existingPlugins.plus(scalaCompilerPluginsConfiguration))
        .getOrElse(scalaCompilerPluginsConfiguration)
      scalaCompile.setScalaCompilerPlugins(plugins)

// TODO - this is reported by `./gradlew resolvableConfigurations` even without me touching anything:
// Consumable configurations with identical capabilities within a project
// (other than the default configuration) must have unique attributes,
// but configuration ':incrementalScalaAnalysisFormain' and [configuration ':incrementalScalaAnalysisElements']
// contain identical attribute sets.
// Consider adding an additional attribute to one of the configurations to disambiguate them.
// For more information, please refer to
// https://docs.gradle.org/8.13/userguide/upgrading_version_7.html#unique_attribute_sets
// in the Gradle documentation.
