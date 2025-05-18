package org.podval.tools.scalajsplugin

import org.gradle.api.{Action, Project}
import org.gradle.api.artifacts.{Configuration, ConfigurationContainer}
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.{SourceSet, TaskContainer, TaskProvider}
import org.podval.tools.build.{BackendDependencyRequirements, CreateExtension, DependencyRequirement, Gradle,
  ScalaBackend, ScalaLibrary, ScalaPlatform}
import org.podval.tools.scalajsplugin.jvm.JvmDelegate
import org.podval.tools.scalajsplugin.scalajs.ScalaJSDelegate
import org.podval.tools.scalajsplugin.scalanative.ScalaNativeDelegate
import org.slf4j.{Logger, LoggerFactory}
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala, SeqHasAsJava}
import java.io.File

trait BackendDelegate[T <: BackendTask]:
  def backend: ScalaBackend

  // TODO capture class tag or something
  def taskClass: Class[? <: T]

  def runTaskClass        :        Class[? <: T & BackendTask.Run.Main]
  def testTaskClass       :        Class[? <: T & BackendTask.Run.Test]

  def linkTaskClassOpt    : Option[Class[? <: T & BackendTask.Link.Main]]
  def testLinkTaskClassOpt: Option[Class[? <: T & BackendTask.Link.Test]]

  def pluginDependenciesConfigurationNameOpt: Option[String]
  def createExtension: Option[CreateExtension[?]]

  final def registerTasks(
    tasks: TaskContainer,
    mainSourceSet: SourceSet,
    testSourceSet: SourceSet
  ): Unit =
    // TODO unify task description here and for scaladoc task with the javadoc style...
    // TODO make utility methods for adding/replacing/configuring tasks...
    
    // Create 'link' task.
    val runTaskDependency: Option[TaskProvider[?]] =
      linkTaskClassOpt.map((linkTaskClass: Class[? <: BackendTask]) =>
        tasks.withType(linkTaskClass).configureEach((task: BackendTask) =>
          task.setDescription(s"Links ${backend.displayName} code.")
          task.setGroup("build")
        )
        tasks.register(
          GradleFeatures.linkTaskName(mainSourceSet),
          linkTaskClass
        )
      )

    // Create 'run' task. - TODO only if it does not exist!
    tasks.withType(runTaskClass).configureEach((task: BackendTask.Main) =>
      task.setDescription(s"Runs ${backend.displayName} code.")
      task.setGroup("other")
    )
    tasks.register(
      GradleFeatures.runTaskName(mainSourceSet),
      runTaskClass,
      new Action[BackendTask.Main]:
        override def execute(runTask: BackendTask.Main): Unit = runTaskDependency.foreach(runTask.dependsOn(_))
    )

    // Create 'testLink' task.
    val testTaskDependency: Option[TaskProvider[?]] =
      testLinkTaskClassOpt.map((testLinkTaskClass: Class[? <: BackendTask]) =>
        tasks.withType(testLinkTaskClass).configureEach((task: BackendTask) =>
          task.setDescription(s"Links test ${backend.displayName} code.")
          task.setGroup("build")
        )
        tasks.register(
          GradleFeatures.linkTaskName(testSourceSet),
          testLinkTaskClass
        )
      )

    // Create 'test' task.
    tasks.withType(testTaskClass).configureEach((task: BackendTask.Test) =>
      task.setDescription(s"Test ${backend.displayName} code using sbt frameworks.")
    )
    val testTask: BackendTask.Test = tasks.replace(
      testSourceSet.getName, // test task and test source set are named the same
      testTaskClass
    )
    testTaskDependency.foreach(testTask.dependsOn(_))

  final def configure(
    project: Project,
    projectScalaLibrary: ScalaLibrary,
    pluginScalaPlatform: ScalaPlatform,
    mainSourceSet: SourceSet,
    testSourceSet: SourceSet
  ): Unit =
    val configurations: ConfigurationContainer = project.getConfigurations
    def getConfiguration(name: String): Configuration = configurations.getByName(name)
    
    val projectScalaPlatform: ScalaPlatform = projectScalaLibrary.toPlatform(backend)

    GradleFeatures.configureJar(
      project,
      mainSourceSet.getJarTaskName,
      archiveAppendixConvention = backend.suffixString + projectScalaLibrary.suffixString
    )

    val implementationConfiguration: Configuration = getConfiguration(
      mainSourceSet.getImplementationConfigurationName
    )
    val testImplementationConfiguration: Configuration = getConfiguration(
      testSourceSet.getImplementationConfigurationName
    )

    val scalaCompilerPluginsConfiguration: Configuration = getConfiguration(
      GradleFeatures.scalaCompilerPluginsConfigurationName(mainSourceSet)
    )

    val requirements: BackendDependencyRequirements = backend.dependencyRequirements(
      implementationConfiguration = implementationConfiguration,
      testImplementationConfiguration = testImplementationConfiguration,
      projectScalaPlatform = projectScalaPlatform
    )

    def applyDependencyRequirements(
      // Arrays are used all the way to here for Scala 2.12 compatibility :(
      dependencyRequirements: Array[DependencyRequirement[ScalaPlatform]],
      scalaPlatform: ScalaPlatform,
      configuration: Configuration
    ): Unit = dependencyRequirements.map(_.applyToConfiguration(
      project,
      configuration,
      scalaPlatform
    ))

    applyDependencyRequirements(
      requirements.implementation,
      projectScalaPlatform,
      implementationConfiguration
    )
    applyDependencyRequirements(
      requirements.testImplementation,
      projectScalaPlatform,
      testImplementationConfiguration
    )
    applyDependencyRequirements(
      requirements.scalaCompilerPlugins,
      projectScalaPlatform,
      scalaCompilerPluginsConfiguration
    )
    pluginDependenciesConfigurationNameOpt.foreach((pluginDependenciesConfigurationName: String) =>
      applyDependencyRequirements(
        requirements.pluginDependencies,
        pluginScalaPlatform,
        getConfiguration(pluginDependenciesConfigurationName)
      )
    )

    val scalaCompileParameters: Seq[String] = backend.scalaCompileParameters(projectScalaLibrary.isScala3)

    def configureScalaCompile(
      sourceSet: SourceSet,
    ): Unit =
      val scalaCompile: ScalaCompile = Gradle // TODO use task name!
        .getClassesTaskProvider(project, sourceSet)
        .get
        .getDependsOn
        .asScala
        .find(classOf[TaskProvider[ScalaCompile]].isInstance)
        .get
        .asInstanceOf[TaskProvider[ScalaCompile]]
        .get

      BackendDelegate.ensureParameters(
        scalaCompile,
        scalaCompileParameters
      )

      BackendDelegate.addScalaCompilerPlugins(
        scalaCompilerPluginsConfiguration,
        scalaCompile
      )

    configureScalaCompile(mainSourceSet)
    configureScalaCompile(testSourceSet)

object BackendDelegate:
  private val logger: Logger = LoggerFactory.getLogger(classOf[BackendDelegate[?]])

  val all: Set[BackendDelegate[?]] = Set(
    JvmDelegate,
    ScalaJSDelegate,
    ScalaNativeDelegate
  )

  private def ensureParameters(
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
  
  private def addScalaCompilerPlugins(
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
