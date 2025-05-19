package org.podval.tools.scalajsplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.provider.ProviderInternal
import org.gradle.api.plugins.{JavaPluginExtension, JvmTestSuitePlugin}
import org.gradle.api.{Action, Project, Task}
import org.gradle.api.plugins.jvm.internal.JvmFeatureInternal
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.{ScalaSourceDirectorySet, SourceSet, SourceSetContainer, TaskProvider}
import org.gradle.jvm.tasks.Jar
import org.gradle.util.internal.GUtil
import org.podval.tools.build.ScalaLibrary
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

  private def getSourceSets(project: Project) = project.getExtensions.getByType(classOf[JavaPluginExtension]).getSourceSets
  def getMainSourceSet(project: Project): SourceSet = getSourceSets(project).getByName("main") // TODO use official constant
  def getTestSourceSet(project: Project): SourceSet = getSourceSets(project).getByName(JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME)

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
      val it = o.isInstanceOf[File] && o.asInstanceOf[File].getAbsolutePath == sourceDirectory.getAbsolutePath
//      if it then println(s"removing $o")
      it
    )
    scalaSourceDirectorySet.setSrcDirs(sourceFiltered.asJava)

  // TODO move to plugin
  def addSharedSources(
    project: Project,
    sharedSibling: Project,
    isRunningInIntelliJ: Boolean
  ): Unit =
    def scalaSources(isTest: Boolean) = Files.file(
      sharedSibling.getProjectDir,
      "src",
      if isTest then "test" else "main",
      "scala"
    )

    // TODO unfold
    def addScalaSourcesBoth(): Unit =
      addScalaSources(getMainSourceSet(project), scalaSources(isTest = false))
      addScalaSources(getTestSourceSet(project), scalaSources(isTest = true ))

    // Permanently.
    if !isRunningInIntelliJ then addScalaSourcesBoth() else
      // Add before compilation and remove after, so that during project import IntelliJ does not
      // run into "duplicate content roots" issue.
      project.getTasks.withType(classOf[ScalaCompile]).configureEach: (scalaCompile: ScalaCompile) =>
        scalaCompile.doFirst(new Action[Task]:
          override def execute(t: Task): Unit = addScalaSourcesBoth()
        )
        scalaCompile.doLast(new Action[Task]:
          override def execute(t: Task): Unit =
            removeScalaSources(getMainSourceSet(project), scalaSources(isTest = false))
            removeScalaSources(getTestSourceSet(project), scalaSources(isTest = true ))
        )

  private def removeAllScalaSources(sourceSet: SourceSet): Unit =
    getScalaSourceDirectorySet(sourceSet).setSrcDirs(List.empty.asJava)

  def removeAllScalaSources(project: Project): Unit =
    removeAllScalaSources(getMainSourceSet(project))
    removeAllScalaSources(getTestSourceSet(project))

  def addScalaLibraryDependency(
    project: Project,
    projectScalaLibrary: ScalaLibrary
  ): Unit =
    project.getDependencies.add(
      getMainSourceSet(project).getImplementationConfigurationName,
      projectScalaLibrary.dependencyWithVersion.dependencyNotation
    )

  // TODO move to plugin
  def configureJar(
    project: Project, 
    jarTaskName: String,
    archiveAppendixConvention: String
  ): Unit =
    project.getTasks.withType(classOf[Jar]).named(jarTaskName).configure((jar: Jar) =>
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
