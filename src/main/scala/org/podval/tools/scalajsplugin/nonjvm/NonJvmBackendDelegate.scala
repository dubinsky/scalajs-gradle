package org.podval.tools.scalajsplugin.nonjvm

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.{SourceSet, TaskProvider}
import org.gradle.api.{Action, Project, Task}
import org.podval.tools.build.{DependencyRequirement, Gradle, ScalaDependency, ScalaLibrary, ScalaPlatform, Version}
import org.podval.tools.scalajsplugin.{AddTestTask, AddToClassPath, BackendDelegate}
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala, SeqHasAsJava, SetHasAsScala}

abstract class NonJvmBackendDelegate(
  project: Project,
  isModeMixed: Boolean
) extends BackendDelegate(
  project,
  isModeMixed
):
  protected def linkMainTaskClass: Class[? <: BackendLinkMainTask[?]]
  protected def linkTestTaskClass: Class[? <: BackendLinkTestTask[?]]
  protected def runMainTaskClass : Class[? <: BackendRunMainTask [?]]
  protected def testTaskClass    : Class[? <: BackendTestTask    [?]]

  protected def createExtensions(): Unit
  protected def pluginDependenciesConfiguration: String
  protected def areCompilerPluginsBuiltIntoScala3: Boolean
  protected def scalaCompileParameters(isScala3: Boolean): Seq[String]
  protected def backendVersionExtractor(version: Version): Version
  protected def backendVersionComposer(projectScalaVersion: Version, backendVersion: Version): Version

  protected def backendVersionDependency(isScala3: Boolean): ScalaDependency.Maker
  protected def compilerScalaCompilerPluginDependency: ScalaDependency.Maker
  protected def linkerDependency: ScalaDependency.Maker
  protected def testAdapterDependency: ScalaDependency.Maker
  protected def testBridgeDependency: ScalaDependency.Maker
  protected def junit4dependency: ScalaDependency.Maker
  protected def junit4ScalaCompilerPluginDependency: ScalaDependency.Maker
  protected def additionalPluginDependencyRequirements: DependencyRequirements

  protected def implementationDependencyRequirements(
    backendVersion: Version,
    projectScalaPlatform: ScalaPlatform
  ): DependencyRequirements

  final override protected def isCreateForMixedMode: Boolean = true

  final override protected def createConfigurations(): Unit =
    // TODO use new one-shot methods
    val configuration: Configuration = project.getConfigurations.create(pluginDependenciesConfiguration)
    configuration.setVisible(false)
    configuration.setCanBeConsumed(false)
    configuration.setDescription(s"${kind.backendKind.displayName} dependencies used by the ScalaJS plugin.")

  final override protected def setUpProject(): AddTestTask[BackendTestTask[?]] =
    createExtensions()

    val linkMain: TaskProvider[? <: BackendLinkTask[?]] = project.getTasks.register(
      "link", // TODO gradleNames
      linkMainTaskClass,
      new Action[BackendLinkTask[?]]:
        override def execute(linkTask: BackendLinkTask[?]): Unit =
          configureLinkTask(linkTask, gradleNames.mainSourceSetName)
    )

    project.getTasks.register(
      "run", // TODO gradleNames
      runMainTaskClass,
      new Action[BackendRunMainTask[?]]:
        override def execute(run: BackendRunMainTask[?]): Unit = run.dependsOn(linkMain.get)
    )

    val linkTest: TaskProvider[? <: BackendLinkTask[?]] = project.getTasks.register(
      "testLink", // TODO gradleNames
      linkTestTaskClass,
      new Action[BackendLinkTask[?]]:
        override def execute(linkTask: BackendLinkTask[?]): Unit =
          configureLinkTask(linkTask, gradleNames.testSourceSetName)
    )

    AddTestTask[BackendTestTask[?]](
      testTaskClass,
      (testTask: BackendTestTask[?]) => testTask.dependsOn(linkTest.get)
    )

  private def configureLinkTask(linkTask: BackendLinkTask[?], sourceSetName: String): Unit =
    val sourceSet: SourceSet = Gradle.getSourceSet(project, sourceSetName)
    // TODO who is going to set runtime classpath for the additional tasks?
    linkTask.getRuntimeClassPath.setFrom(sourceSet.getRuntimeClasspath)
    linkTask.dependsOn(Gradle.getClassesTask(project, sourceSet))

  final override protected def afterEvaluate(
    pluginScalaPlatform: ScalaPlatform,
    projectScalaLibrary: ScalaLibrary,
    projectScalaPlatform: ScalaPlatform
  ): Option[AddToClassPath] =
    val isScala3: Boolean = projectScalaPlatform.version.isScala3

    val backendDependency: ScalaDependency.Maker = backendVersionDependency(isScala3)

    val backendVersion: Version = backendDependency
      .findInConfiguration(projectScalaPlatform, project, gradleNames.implementationConfigurationName)
      .map(_.version)
      .map(backendVersionExtractor)
      .getOrElse(backendDependency.versionDefault)

    val backendVersionDependencyRequirement: DependencyRequirement[ScalaPlatform] = backendDependency
      .required(backendVersionComposer(projectScalaPlatform.scalaVersion, backendVersion))

    applyDependencyRequirements(
      Seq(backendVersionDependencyRequirement) ++
      implementationDependencyRequirements(backendVersion, projectScalaPlatform),
      projectScalaPlatform,
      gradleNames.implementationConfigurationName
    )

    applyDependencyRequirements(
      Seq(linkerDependency, testAdapterDependency).map(_.required(backendVersion)) ++
      additionalPluginDependencyRequirements,
      pluginScalaPlatform,
      pluginDependenciesConfiguration
    )

    if !areCompilerPluginsBuiltIntoScala3 || !isScala3 then
      applyDependencyRequirements(
        Seq(compilerScalaCompilerPluginDependency.required(backendVersion)),
        projectScalaPlatform,
        gradleNames.scalaCompilerPluginsConfigurationName
      )

      // only when JUnit4 is in use (without JUnit on classpath, JUnit4 compiler plugin causes compiler errors).
      val isJUnit4Present: Boolean = junit4dependency
        .findInConfiguration(projectScalaPlatform, project, gradleNames.testImplementationConfigurationName)
        .isDefined
  
      if isJUnit4Present then
        applyDependencyRequirements(
          Seq(junit4ScalaCompilerPluginDependency.required(backendVersion)),
          projectScalaPlatform,
          gradleNames.scalaCompilerPluginsConfigurationName
        )

    applyDependencyRequirements(
      Seq(testBridgeDependency.required(backendVersion)),
      projectScalaPlatform,
      gradleNames.testImplementationConfigurationName
    )

    // TODO disable compileJava task for the Scala.js sourceSet - unless Scala.js compiler deals with Java classes?
    configureScalaCompile(gradleNames.mainSourceSetName, isScala3)
    configureScalaCompile(gradleNames.testSourceSetName, isScala3)

    Some(
      AddToClassPath(
        pluginDependenciesConfiguration,
        projectScalaLibrary,
        gradleNames.runtimeClasspathConfigurationName
      )
    )

  private def configureScalaCompile(
    sourceSetName: String,
    isScala3: Boolean
  ): Unit =
    val scalaCompile: ScalaCompile = getScalaCompile(sourceSetName)

    ensureParameters(
      scalaCompile,
      sourceSetName,
      scalaCompileParameters(isScala3)
    )

    addScalaCompilerPlugins(
      scalaCompile,
      sourceSetName
    )

  private def ensureParameters(
    scalaCompile: ScalaCompile,
    sourceSetName: String,
    toAdd: Seq[String]
  ): Unit =
    val parameters: List[String] = Option(scalaCompile.getScalaCompileOptions.getAdditionalParameters) // nullable
      .map(_.asScala.toList)
      .getOrElse(List.empty)

    val parametersNew: List[String] = toAdd.foldLeft(parameters) {
      case (parameters, parameter) =>
        if parameters.contains(parameter) then parameters else
          BackendDelegate.logger.info(s"scalaCompileOptions.additionalParameters of the $sourceSetName ScalaCompile task: adding '$parameter'.")
          parameters :+ parameter
    }

    scalaCompile
      .getScalaCompileOptions
      .setAdditionalParameters(parametersNew.asJava)

  private def addScalaCompilerPlugins(
    scalaCompile: ScalaCompile,
    sourceSetName: String
  ): Unit =
    // There seems to be no need to add `"-Xplugin:" + plugin.getPath` parameters:
    // just adding plugins to the list is sufficient.
    val scalaCompilerPlugins: FileCollection = Gradle.getConfiguration(project, gradleNames.scalaCompilerPluginsConfigurationName)
    if scalaCompilerPlugins.asScala.nonEmpty then
      BackendDelegate.logger.info(s"scalaCompilerPlugins of the $sourceSetName ScalaCompile task: adding ${scalaCompilerPlugins.asScala}.")
      val plugins: FileCollection = Option(scalaCompile.getScalaCompilerPlugins)
        .map((existingPlugins: FileCollection) => existingPlugins.plus(scalaCompilerPlugins))
        .getOrElse(scalaCompilerPlugins)
      scalaCompile.setScalaCompilerPlugins(plugins)

  private def getScalaCompile(sourceSetName: String): ScalaCompile = Gradle
    .getClassesTask(project, sourceSetName)
    .getDependsOn
    .asScala
    .find(classOf[TaskProvider[ScalaCompile]].isInstance)
    .get
    .asInstanceOf[TaskProvider[ScalaCompile]]
    .get
