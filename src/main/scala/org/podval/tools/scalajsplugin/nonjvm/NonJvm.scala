package org.podval.tools.scalajsplugin.nonjvm

import org.gradle.api.{Action, Project, Task}
import org.gradle.api.tasks.{SourceSet, TaskProvider}
import org.podval.tools.build.{DependencyRequirement, Gradle, ScalaBackendKind, ScalaDependency, ScalaPlatform, Version}
import org.podval.tools.scalajsplugin.{BackendDelegate, GradleNames}

trait NonJvm extends BackendDelegate:
  def linkMainTaskClass: Class[? <: NonJvmLinkMainTask[?]]
  def linkTestTaskClass: Class[? <: NonJvmLinkTestTask[?]]
  def runMainTaskClass : Class[? <: NonJvmRunMainTask [?]]
  def testTaskClass    : Class[? <: NonJvmTestTask    [?]]
  
  def pluginDependenciesConfigurationName: String
  def areCompilerPluginsBuiltIntoScala3: Boolean
  def versionExtractor(version: Version): Version
  def versionComposer(projectScalaVersion: Version, backendVersion: Version): Version
  def library(isScala3: Boolean): ScalaDependency.Maker
  def implementation: Seq[ScalaDependency.Maker]
  def compiler    : ScalaDependency.Maker
  def linker      : ScalaDependency.Maker
  def testAdapter : ScalaDependency.Maker
  def testBridge  : ScalaDependency.Maker
  def junit4Plugin: ScalaDependency.Maker
  def junit4      : ScalaDependency.Maker

  def additionalPluginDependencyRequirements: Seq[DependencyRequirement[ScalaPlatform]]

  def additionalImplementationDependencyRequirements(
    backendVersion: Version,
    projectScalaPlatform: ScalaPlatform
  ): Seq[DependencyRequirement[ScalaPlatform]]

  override def backendKind: ScalaBackendKind.NonJvm

  final override def pluginDependenciesConfigurationNameOpt: Option[String] = Some(pluginDependenciesConfigurationName)
  
  final override def addTasks(project: Project, gradleNames: GradleNames): Option[TaskProvider[? <: Task]] =
    project.getTasks.withType(linkMainTaskClass).configureEach((task: NonJvmLinkMainTask[?]) =>
      task.setDescription(s"Links ${backendKind.displayName} code.")
      task.setGroup("build")
      val sourceSet: SourceSet = Gradle.getSourceSet(project, gradleNames.mainSourceSetName)
      task.dependsOn(Gradle.getClassesTaskProvider(project, sourceSet))
      task.getRuntimeClassPath.setFrom(sourceSet.getRuntimeClasspath)
    )
    project.getTasks.withType(linkTestTaskClass).configureEach((task: NonJvmLinkTestTask[?]) =>
      task.setDescription(s"Links test ${backendKind.displayName} code.")
      task.setGroup("build")
      val sourceSet: SourceSet = Gradle.getSourceSet(project, gradleNames.testSourceSetName)
      task.dependsOn(Gradle.getClassesTaskProvider(project, sourceSet))
      task.getRuntimeClassPath.setFrom(sourceSet.getRuntimeClasspath)
    )
    project.getTasks.withType(runMainTaskClass).configureEach((task: NonJvmRunMainTask[?]) =>
      task.setDescription(s"Runs ${backendKind.displayName} code.")
      task.setGroup("other")
    )

    val linkMain: TaskProvider[? <: NonJvmLinkTask[?]] = project.getTasks.register(
      gradleNames.linkTaskName,
      linkMainTaskClass
    )

    project.getTasks.register(
      gradleNames.runTaskName,
      runMainTaskClass,
      new Action[NonJvmRunMainTask[?]]:
        override def execute(task: NonJvmRunMainTask[?]): Unit = task.dependsOn(linkMain)
    )

    val linkTest: TaskProvider[? <: NonJvmLinkTask[?]] = project.getTasks.register(
      gradleNames.testLinkTaskName,
      linkTestTaskClass
    )

    Some(linkTest)

  final override def applyDependencyRequirements(
    project: Project,
    gradleNames: GradleNames,
    pluginScalaPlatform: ScalaPlatform,
    projectScalaPlatform: ScalaPlatform,
    isScala3: Boolean
  ): Unit =
    val implementationConfigurationName: String = Gradle
      .getSourceSet(project, gradleNames.mainSourceSetName)
      .getImplementationConfigurationName

    val libraryDependency: ScalaDependency.Maker = library(isScala3)

    val backendVersion: Version = libraryDependency
      .findInConfiguration(projectScalaPlatform, project, implementationConfigurationName)
      .map(_.version)
      .map(versionExtractor)
      .getOrElse(libraryDependency.versionDefault)

    val libraryDependencyRequirement: DependencyRequirement[ScalaPlatform] = libraryDependency
      .required(versionComposer(projectScalaPlatform.scalaVersion, backendVersion))

    BackendDelegate.applyDependencyRequirements(
      project,
      Seq(libraryDependencyRequirement) ++
      implementation.map(_.required(backendVersion)) ++
      additionalImplementationDependencyRequirements(backendVersion, projectScalaPlatform),
      projectScalaPlatform,
      implementationConfigurationName
    )

    BackendDelegate.applyDependencyRequirements(
      project,
      Seq(linker, testAdapter).map(_.required(backendVersion)) ++
      additionalPluginDependencyRequirements,
      pluginScalaPlatform,
      pluginDependenciesConfigurationName
    )

    val testImplementationConfigurationName: String = Gradle
      .getSourceSet(project, gradleNames.testSourceSetName)
      .getImplementationConfigurationName

    if !areCompilerPluginsBuiltIntoScala3 || !isScala3 then
      // Add JUnit4 compiler plugin only when JUnit4 is in use, otherwise with Scala.js `testClasses` task throws
      //   "scala.reflect.internal.MissingRequirementError: object org.junit.Test in compiler mirror not found.";
      // somehow, `classes` task works fine, so there is no need, it seems, to create for a separate configuration
      //   `testScalaCompilerPlugins` (like Scala Native SBT plugin does).
      val isJUnit4Present: Boolean = junit4
        .findInConfiguration(projectScalaPlatform, project, testImplementationConfigurationName)
        .isDefined

      val plugins: Seq[ScalaDependency.Maker] =
        Seq(compiler) ++
        (if !isJUnit4Present then Seq.empty else Seq(junit4Plugin))

      BackendDelegate.applyDependencyRequirements(
        project,
        plugins.map(_.required(backendVersion)),
        projectScalaPlatform,
        gradleNames.scalaCompilerPluginsConfigurationName
      )

    BackendDelegate.applyDependencyRequirements(
      project,
      Seq(testBridge.required(backendVersion)),
      projectScalaPlatform,
      testImplementationConfigurationName
    )
