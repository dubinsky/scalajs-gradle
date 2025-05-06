package org.podval.tools.scalajsplugin.nonjvm

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.{SourceSet, TaskProvider}
import org.gradle.api.{Action, Project, Task}
import org.podval.tools.build.{DependencyRequirement, Gradle, ScalaDependency, ScalaLibrary, ScalaPlatform, Version}
import org.podval.tools.scalajsplugin.{AddTestTask, AddToClassPath, BackendDelegate}
import java.io.File
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala, SeqHasAsJava, SetHasAsScala}

abstract class NonJvmDelegate(
  project: Project,
  isModeMixed: Boolean
) extends BackendDelegate(
  project,
  isModeMixed
):
  protected def linkMainTaskClass: Class[? <: NonJvmLinkMainTask[?]]
  protected def linkTestTaskClass: Class[? <: NonJvmLinkTestTask[?]]
  protected def runMainTaskClass : Class[? <: NonJvmRunMainTask [?]]
  protected def testTaskClass    : Class[? <: NonJvmTestTask    [?]]

  protected def createExtensions(): Unit
  protected def pluginDependenciesConfigurationName: String
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
    // TODO use new one-shot methods resolvable()/consumable() etc.
    val configuration: Configuration = project.getConfigurations.create(pluginDependenciesConfigurationName)
    configuration.setVisible(false)
    configuration.setCanBeConsumed(false)
    configuration.setDescription(s"${kind.backendKind.displayName} dependencies used by the ScalaJS plugin.")

  final override protected def setUpProject(): AddTestTask[NonJvmTestTask[?]] =
    createExtensions()

    val linkMain: TaskProvider[? <: NonJvmLinkTask[?]] = project.getTasks.register(
      gradleNames.linkTaskName,
      linkMainTaskClass,
      new Action[NonJvmLinkTask[?]]:
        override def execute(linkTask: NonJvmLinkTask[?]): Unit =
          configureLinkTask(linkTask, gradleNames.mainSourceSetName)
    )

    project.getTasks.register(
      gradleNames.runTaskName,
      runMainTaskClass,
      new Action[NonJvmRunMainTask[?]]:
        override def execute(run: NonJvmRunMainTask[?]): Unit = run.dependsOn(linkMain)
    )

    val linkTest: TaskProvider[? <: NonJvmLinkTask[?]] = project.getTasks.register(
      gradleNames.testLinkTaskName,
      linkTestTaskClass,
      new Action[NonJvmLinkTask[?]]:
        override def execute(linkTask: NonJvmLinkTask[?]): Unit =
          configureLinkTask(linkTask, gradleNames.testSourceSetName)
    )

    AddTestTask[NonJvmTestTask[?]](
      testTaskClass,
      (testTask: NonJvmTestTask[?]) => testTask.dependsOn(linkTest)
    )

  private def configureLinkTask(linkTask: NonJvmLinkTask[?], sourceSetName: String): Unit =
    val sourceSet: SourceSet = Gradle.getSourceSet(project, sourceSetName)
    linkTask.dependsOn(Gradle.getClassesTaskProvider(project, sourceSet))
    // TODO to set runtime classpath for the additional tasks, I need to get all `TaskProvider`s
    // with a given type...
    linkTask.getRuntimeClassPath.setFrom(sourceSet.getRuntimeClasspath)

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
      pluginDependenciesConfigurationName
    )

    if !areCompilerPluginsBuiltIntoScala3 || !isScala3 then
      // Add JUnit4 compiler plugin only when JUnit4 is in use, otherwise with Scala.js `testClasses` task throws
      //   "scala.reflect.internal.MissingRequirementError: object org.junit.Test in compiler mirror not found.";
      // somehow, `classes` task works fine, so there is no need, it seems, to create for a separate configuration
      //   `testScalaCompilerPlugins` (like Scala Native SBT plugin does).
      val isJUnit4Present: Boolean = junit4dependency
        .findInConfiguration(projectScalaPlatform, project, gradleNames.testImplementationConfigurationName)
        .isDefined

      val plugins: Seq[ScalaDependency.Maker] =
        Seq(compilerScalaCompilerPluginDependency) ++
        (if !isJUnit4Present then Seq.empty else Seq(junit4ScalaCompilerPluginDependency))

      applyDependencyRequirements(
        plugins.map(_.required(backendVersion)),
        projectScalaPlatform,
        gradleNames.scalaCompilerPluginsConfigurationName
      )

    applyDependencyRequirements(
      Seq(testBridgeDependency.required(backendVersion)),
      projectScalaPlatform,
      gradleNames.testImplementationConfigurationName
    )

    configureScalaCompile(gradleNames.mainSourceSetName, isScala3)
    configureScalaCompile(gradleNames.testSourceSetName, isScala3)

    Some(
      AddToClassPath(
        pluginDependenciesConfigurationName,
        projectScalaLibrary,
        gradleNames.runtimeClasspathConfigurationName
      )
    )

  private def configureScalaCompile(
    sourceSetName: String,
    isScala3: Boolean
  ): Unit =
    val scalaCompile: ScalaCompile = Gradle.getScalaCompile(project, sourceSetName)

    ensureParameters(
      scalaCompile,
      sourceSetName,
      scalaCompileParameters(isScala3)
    )

    addScalaCompilerPlugins(scalaCompile)

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

  private def addScalaCompilerPlugins(scalaCompile: ScalaCompile): Unit =
    // There seems to be no need to add `"-Xplugin:" + plugin.getPath` parameters:
    // just adding plugins to the list is sufficient.
    val scalaCompilerPluginsConfiguration: Configuration = Gradle.getConfiguration(
      project,
      gradleNames.scalaCompilerPluginsConfigurationName
    )
    val scalaCompilerPlugins: Iterable[File] = scalaCompilerPluginsConfiguration.asScala
    if scalaCompilerPlugins.nonEmpty then
      BackendDelegate.logger.info(s"Adding ${scalaCompile.getName} scalaCompilerPlugins: $scalaCompilerPlugins.")
      val plugins: FileCollection = Option(scalaCompile.getScalaCompilerPlugins)
        .map((existingPlugins: FileCollection) => existingPlugins.plus(scalaCompilerPluginsConfiguration))
        .getOrElse(scalaCompilerPluginsConfiguration)
      scalaCompile.setScalaCompilerPlugins(plugins)
