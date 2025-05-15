package org.podval.tools.scalajsplugin

import org.gradle.api.{Action, Project}
import org.gradle.api.artifacts.{Configuration, ConfigurationContainer}
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.{SourceSet, TaskContainer, TaskProvider}
import org.podval.tools.build.{CreateExtension, DependencyRequirement, Gradle, ScalaBackendKind, ScalaLibrary,
  ScalaPlatform}
import org.slf4j.{Logger, LoggerFactory}
import java.io.File
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala, SeqHasAsJava}

trait BackendDelegate[T <: BackendTask]:
  // TODO capture class tag or something
  def taskClass: Class[? <: T]

  def runTaskClass        :        Class[? <: T & BackendTask.Run.Main]
  def testTaskClass       :        Class[? <: T & BackendTask.Run.Test]

  def linkTaskClassOpt    : Option[Class[? <: T & BackendTask.Link.Main]]
  def testLinkTaskClassOpt: Option[Class[? <: T & BackendTask.Link.Test]]

  final protected def describe(what: String): String = s"${backendKind.displayName} $what."

  def backendKind: ScalaBackendKind
  def pluginDependenciesConfigurationNameOpt: Option[String]
  def scalaCompileParameters(isScala3: Boolean): Seq[String]
  def createExtension: Option[CreateExtension[?]]

  def dependencyRequirements(
    implementationConfiguration: Configuration,
    testImplementationConfiguration: Configuration,
    projectScalaPlatform: ScalaPlatform
  ): BackendDependencyRequirements

  final def registerTasks(
    tasks: TaskContainer,
    mainSourceSet: SourceSet,
    testSourceSet: SourceSet
  ): Unit =
    // TODO unify task description here and for scaladoc task with the javadoc style...
    
    // Create 'link' task.
    val runTaskDependency: Option[TaskProvider[?]] =
      linkTaskClassOpt.map((linkTaskClass: Class[? <: BackendTask]) =>
        tasks.withType(linkTaskClass).configureEach((task: BackendTask) =>
          task.setDescription(s"Links ${backendKind.displayName} code.")
          task.setGroup("build")
        )
        tasks.register(
          GradleNames.linkTaskName(mainSourceSet),
          linkTaskClass
        )
      )

    // Create 'run' task.
    tasks.withType(runTaskClass).configureEach((task: BackendTask.Main) =>
      task.setDescription(s"Runs ${backendKind.displayName} code.")
      task.setGroup("other")
    )
    tasks.register(
      GradleNames.runTaskName(mainSourceSet),
      runTaskClass,
      new Action[BackendTask.Main]:
        override def execute(runTask: BackendTask.Main): Unit = runTaskDependency.foreach(runTask.dependsOn(_))
    )

    // Create 'testLink' task.
    val testTaskDependency: Option[TaskProvider[?]] =
      testLinkTaskClassOpt.map((testLinkTaskClass: Class[? <: BackendTask]) =>
        tasks.withType(testLinkTaskClass).configureEach((task: BackendTask) =>
          task.setDescription(s"Links test ${backendKind.displayName} code.")
          task.setGroup("build")
        )
        tasks.register(
          GradleNames.linkTaskName(testSourceSet),
          testLinkTaskClass
        )
      )

    // Create 'test' task.
    tasks.withType(testTaskClass).configureEach((task: BackendTask.Test) =>
      task.setDescription(s"Test ${backendKind.displayName} code using sbt frameworks.")
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
    
    val projectScalaPlatform: ScalaPlatform = projectScalaLibrary.toPlatform(backendKind)

    GradleFeatures.configureJar(
      project,
      mainSourceSet.getJarTaskName,
      archiveAppendixConvention = backendKind.suffixString + projectScalaLibrary.suffixString
    )

    val implementationConfiguration: Configuration = getConfiguration(
      mainSourceSet.getImplementationConfigurationName
    )
    val testImplementationConfiguration: Configuration = getConfiguration(
      testSourceSet.getImplementationConfigurationName
    )

    val scalaCompilerPluginsConfiguration: Configuration = getConfiguration(
      GradleNames.scalaCompilerPluginsConfigurationName(mainSourceSet)
    )

    val requirements: BackendDependencyRequirements = dependencyRequirements(
      implementationConfiguration = implementationConfiguration,
      testImplementationConfiguration = testImplementationConfiguration,
      projectScalaPlatform = projectScalaPlatform
    )

    def applyDependencyRequirements(
      dependencyRequirements: Seq[DependencyRequirement[ScalaPlatform]],
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

    val scalaCompileParametersToAdd: Seq[String] = scalaCompileParameters(projectScalaLibrary.isScala3)

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
        scalaCompileParametersToAdd
      )

      BackendDelegate.addScalaCompilerPlugins(
        scalaCompilerPluginsConfiguration,
        scalaCompile
      )

    configureScalaCompile(mainSourceSet)
    configureScalaCompile(testSourceSet)

object BackendDelegate:
  private val logger: Logger = LoggerFactory.getLogger(classOf[BackendDelegate[?]])

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
