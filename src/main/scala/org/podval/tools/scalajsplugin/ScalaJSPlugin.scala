package org.podval.tools.scalajsplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.{ExtraPropertiesExtension, JavaBasePlugin}
import org.gradle.api.plugins.scala.{ScalaBasePlugin, ScalaPlugin}
import org.gradle.api.tasks.{SourceSet, TaskContainer, TaskProvider}
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.{Action, Plugin, Project}
import org.podval.tools.build.jvm.JvmBackend
import org.podval.tools.build.{AddConfigurationToClassPath, BackendDependencyRequirements, GradleClassPath,
  ScalaBackend, ScalaLibrary, ScalaPlatform}
import org.podval.tools.scalajsplugin.jvm.JvmDelegate
import org.podval.tools.scalajsplugin.scalajs.ScalaJSDelegate
import org.podval.tools.scalajsplugin.scalanative.ScalaNativeDelegate
import org.podval.tools.test.task.{IntelliJIdea, TestTask}
import java.io.File

// TODO include backend-specific subprojects and the shared subproject if it exists -
// do I need to apply as a Settings plugin?!
final class ScalaJSPlugin extends Plugin[Project]:
  override def apply(project: Project): Unit =
    // Apply Scala plugin to this project.
    project.getPluginManager.apply(classOf[ScalaPlugin])

    val subprojectBackends: Set[ScalaBackend] = ScalaJSPlugin.subprojectBackends(project)
    val delegateOpt: Option[BackendDelegate[?]] = ScalaJSPlugin.delegateOpt(project)
    val isModeMixed: Boolean = delegateOpt.isEmpty && subprojectBackends.nonEmpty

    ScalaJSPlugin.apply(
      project,
      subprojectBackends,
      delegate = delegateOpt.getOrElse(JvmDelegate),
      isModeMixed = isModeMixed,
      includeShared = !isModeMixed && ScalaJSPlugin.includeShared(project),
      isRunningInIntelliJ = IntelliJIdea.runningIn
    )

object ScalaJSPlugin:
  val backendProperty: String = "org.podval.tools.scalajs.backend"

  private def delegateOpt(project: Project): Option[BackendDelegate[?]] =
    Option(project.findProperty(backendProperty))
      .map(_.toString)
      .map((name: String) => Set(JvmDelegate, ScalaJSDelegate, ScalaNativeDelegate)
        .find(_.backend.name == name)
        .getOrElse(throw IllegalArgumentException(s"Unknown backend '$name'."))
      )

  val includeSharedProperty: String = "org.podval.tools.scalajs.includeShared"

  private def includeShared(project: Project): Boolean =
    Option(project.findProperty(includeSharedProperty))
      .map(_.toString)
      .contains("true")

  private def subprojectBackends(project: Project): Set[ScalaBackend] = ScalaBackend
    .all
    .filter((backend: ScalaBackend) =>
      val file: File = project.file(backend.sourceRoot)
      file.exists && file.isDirectory
    )

  private def taskName(name: String, sourceSet: SourceSet) = sourceSet.getTaskName(name, "")
  private def scalaCompilerPluginsConfigurationName(sourceSet: SourceSet): String =
    taskName(ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME, sourceSet)

  def apply(
    project: Project,
    subprojectBackends: Set[ScalaBackend],
    delegate: BackendDelegate[?],
    isModeMixed: Boolean,
    includeShared: Boolean,
    isRunningInIntelliJ: Boolean
  ): Unit =
    val mode: String = if isModeMixed then "mixed" else if includeShared then "including shared" else "single"
    val backendString: String = if isModeMixed then "" else s" on ${delegate.backend.displayName}"
    val ijString: String = if isRunningInIntelliJ then " [IJ]" else ""
    project.getLogger.lifecycle(s"ScalaJSPlugin[${project.getName}]: $mode$backendString$ijString.")

    val sharedSubproject: Option[Project] = Gradle.findSubproject(project, ScalaBackend.sharedSourceRoot)

    if isModeMixed then
      // Apply 'scala' plugin to the shared project (if it exists).
      sharedSubproject.foreach(_.getPluginManager.apply("scala"))

      // Remove all Scala sources from the overall project.
      // TODO is it possible/desirable to include the sources of the parent project as shared?
      Gradle.removeAllScalaSources(project)

      // Disable all tasks in the shared subproject and in the overall project.
      Gradle.disableAllTasks(project)
      sharedSubproject.foreach(Gradle.disableAllTasks)

      for subprojectBackend: ScalaBackend <- subprojectBackends do configureSubproject(
        subproject = Gradle.findSubproject(project, subprojectBackend.sourceRoot).get, 
        backendName = subprojectBackend.name
      )
    else
      if includeShared then
        val sharedSibling: Project = project.getParent.project(ScalaBackend.sharedSourceRoot)
        Gradle.addSharedSources(project, sharedSibling, isRunningInIntelliJ)
        if isRunningInIntelliJ then addDependencyOnTheSharedSibling(project, sharedSibling)

      configureTestTask(project)
      configureRuntimeAndClassesTasks(project, delegate)
      delegate.createExtension.foreach(_.create(project))
      createPluginDependenciesConfiguration(project, delegate)
      registerTasks(project, delegate)

    project.afterEvaluate: (project: Project) =>
      val projectScalaLibrary: ScalaLibrary = ScalaLibrary
       .getFromConfiguration(project.getConfigurations.getByName(Gradle.getMainSourceSet(project).getImplementationConfigurationName))

      if isModeMixed then
        sharedSubproject.foreach(Gradle.addScalaLibraryDependency(_, projectScalaLibrary))
        for subprojectBackend: ScalaBackend <- subprojectBackends do Gradle.addScalaLibraryDependency(
          project = Gradle.findSubproject(project, subprojectBackend.sourceRoot).get, 
          projectScalaLibrary
        )
      else 
        val pluginScalaPlatform: ScalaPlatform = ScalaLibrary.getFromClasspath(GradleClassPath.collect(this)).toPlatform(JvmBackend)
        configureJarTask(project, delegate, projectScalaLibrary)
        // TODO configure scaladoc and sources archives.
        applyDependencyRequirements(project, delegate, projectScalaLibrary, pluginScalaPlatform)
        configureScalaCompile(project, delegate, projectScalaLibrary)
        expandClassPath(project, delegate, projectScalaLibrary)

  private def configureSubproject(
    subproject: Project,
    backendName: String
  ): Unit =
    val extraProperties: ExtraPropertiesExtension = subproject.getExtensions.getByType(classOf[ExtraPropertiesExtension])

    // Configure subproject's backend.
    extraProperties.set(backendProperty, backendName)

    // Tell subproject to include shared sources.
    extraProperties.set(includeSharedProperty, "true")

    // Apply the plugin.
    subproject.getPluginManager.apply(classOf[ScalaJSPlugin])

  // TODO exclude this dependency from publications!
  private def addDependencyOnTheSharedSibling(
    project: Project,
    sharedSibling: Project
  ): Unit = project
    .getDependencies
    .add(Gradle.getMainSourceSet(project).getImplementationConfigurationName, sharedSibling)

  private def configureTestTask(project: Project): Unit =
    project.getTasks.withType(classOf[TestTask]).configureEach((testTask: TestTask) =>
      testTask.setGroup(JavaBasePlugin.VERIFICATION_GROUP)
      testTask.useSbt()
    )

  private def configureRuntimeAndClassesTasks(
    project: Project,
    delegate: BackendDelegate[?]
  ): Unit =
    // Set 'runtimeClassPath' and dependency on the 'classes' task.
    project.getTasks.withType(delegate.taskClass).configureEach((task: BackendTask) =>
      def sourceSet: SourceSet = 
        if task.isTest 
        then Gradle.getTestSourceSet(project)
        else Gradle.getMainSourceSet(project)

      if task.isInstanceOf[BackendTask.HasRuntimeClassPath] then
        task.asInstanceOf[BackendTask.HasRuntimeClassPath].getRuntimeClassPath.setFrom(sourceSet.getRuntimeClasspath)
      if task.isInstanceOf[BackendTask.DependsOnClasses] then
        task.asInstanceOf[BackendTask.DependsOnClasses].dependsOn(Gradle.getClassesTaskProvider(project, sourceSet))
    )
  
  private def createPluginDependenciesConfiguration(
    project: Project,
    delegate: BackendDelegate[?]
  ): Unit =
    delegate.pluginDependenciesConfigurationNameOpt.map(configurationName =>
      // TODO use new one-shot methods resolvable()/consumable() etc.
      val configuration: Configuration = project.getConfigurations.create(configurationName)
      configuration.setVisible(false)
      configuration.setCanBeConsumed(false)
      configuration.setDescription(s"${delegate.backend.displayName} dependencies used by the ScalaJS plugin.")
    )

  private def registerTasks(
    project: Project,
    delegate: BackendDelegate[?]
  ): Unit =
    val mainSourceSet: SourceSet = Gradle.getMainSourceSet(project)
    val testSourceSet: SourceSet = Gradle.getTestSourceSet(project)
    
    def linkTaskName(sourceSet: SourceSet): String = taskName("link", sourceSet)
    
    def registerTask[T <: BackendTask](
      clazz: Class[? <: T],
      name: String,
      before: String,
      after: String,
      group: String,
      dependsOn: Option[TaskProvider[?]] = None,
      replace: Boolean = false
    ): TaskProvider[? <: T] =
      project.getTasks.withType(clazz).configureEach((task: BackendTask) =>
        task.setDescription(s"$before ${delegate.backend.displayName} code$after.")
        task.setGroup(group)
      )
      
      // TODO fold with noop action
      val action: Action[T] = (task: T) => dependsOn.foreach(task.dependsOn(_))

      if !replace
      then
        project.getTasks.register(name, clazz, action)
      else
        project.getTasks.replace(name, clazz)
        project.getTasks.withType(clazz).named(name, action)
    
    // Register 'link' task.
    val link: Option[TaskProvider[?]] =
      delegate.linkTaskClassOpt.map((linkTaskClass: Class[? <: BackendTask]) =>
        registerTask(
          clazz = linkTaskClass,
          name = linkTaskName(mainSourceSet), 
          before = "Links ",
          after = "", 
          group = "build"
        )
      )

    // Register 'run' task. - TODO only if it does not exist!
    registerTask(
      clazz = delegate.runTaskClass,
      name = taskName("run", mainSourceSet),
      before = "Runs", 
      after = "", 
      group = "other",
      dependsOn = link
    )

    // Register 'testLink' task.
    val linkTest: Option[TaskProvider[?]] =
      delegate.testLinkTaskClassOpt.map((testLinkTaskClass: Class[? <: BackendTask]) =>
        registerTask(
          clazz = testLinkTaskClass,
          name = linkTaskName(testSourceSet),
          before = "Links test", 
          after = "", 
          group = "build"
        )
      )

    // Replace 'test' task.
    // Test task and test source set are named the same.
    registerTask(
      clazz = delegate.testTaskClass,
      name = testSourceSet.getName, 
      before = "Tests", 
      after = " using sbt frameworks",
      group = "verification",
      dependsOn = linkTest,
      replace = true
    )
  
  private def configureJarTask(
    project: Project,
    delegate: BackendDelegate[?],
    projectScalaLibrary: ScalaLibrary
  ) : Unit =
    val mainSourceSet: SourceSet = Gradle.getMainSourceSet(project)
    Gradle.configureJar(
      project,
      jarTaskName = mainSourceSet.getJarTaskName,
      archiveAppendixConvention = delegate.backend.suffixString + projectScalaLibrary.suffixString
    )
  private def expandClassPath(
    project: Project,
    delegate: BackendDelegate[?],
    projectScalaLibrary: ScalaLibrary
  ): Unit =
    val mainSourceSet: SourceSet = Gradle.getMainSourceSet(project)
    val addToClassPath: Option[AddConfigurationToClassPath] = delegate
      .pluginDependenciesConfigurationNameOpt
      .map((pluginDependenciesConfigurationName: String) =>
        AddConfigurationToClassPath(
          project.getConfigurations.getByName(pluginDependenciesConfigurationName),
          project.getConfigurations.getByName(mainSourceSet.getRuntimeClasspathConfigurationName)
        )
      )

    addToClassPath.foreach(_.add())
    addToClassPath.foreach(_.verify(projectScalaLibrary))

  private def applyDependencyRequirements(
    project: Project,
    delegate: BackendDelegate[?],
    projectScalaLibrary: ScalaLibrary,
    pluginScalaPlatform: ScalaPlatform
  ): Unit =
    val projectScalaPlatform: ScalaPlatform = projectScalaLibrary.toPlatform(delegate.backend)

    val mainSourceSet: SourceSet = Gradle.getMainSourceSet(project)
    val testSourceSet: SourceSet = Gradle.getTestSourceSet(project)

    val implementationConfigurationName    : String = mainSourceSet.getImplementationConfigurationName
    val testImplementationConfigurationName: String = testSourceSet.getImplementationConfigurationName
    
    val requirements: BackendDependencyRequirements = delegate.backend.dependencyRequirements(
      implementationConfiguration = project.getConfigurations.getByName(implementationConfigurationName),
      testImplementationConfiguration = project.getConfigurations.getByName(testImplementationConfigurationName),
      projectScalaPlatform = projectScalaPlatform
    )

    val applications: Seq[ApplyDependencyRequirements] = Seq(
      ApplyDependencyRequirements(
        requirements.implementation,
        projectScalaPlatform,
        implementationConfigurationName
      ),
      ApplyDependencyRequirements(
        requirements.testImplementation,
        projectScalaPlatform,
        testImplementationConfigurationName
      ),
      ApplyDependencyRequirements(
        requirements.scalaCompilerPlugins,
        projectScalaPlatform,
        scalaCompilerPluginsConfigurationName(mainSourceSet)
      )
    ) ++ delegate.pluginDependenciesConfigurationNameOpt.toSeq.map((pluginDependenciesConfigurationName: String) =>
      ApplyDependencyRequirements(
        requirements.pluginDependencies,
        pluginScalaPlatform,
        pluginDependenciesConfigurationName
      )
    )

    applications.foreach(_.apply(project))

  private def configureScalaCompile(
    project: Project,
    delegate: BackendDelegate[?],
    projectScalaLibrary: ScalaLibrary
  ): Unit =
    val mainSourceSet: SourceSet = Gradle.getMainSourceSet(project)
    val testSourceSet: SourceSet = Gradle.getTestSourceSet(project)

    val scalaCompilerPluginsConfiguration: Configuration =
      project.getConfigurations.getByName(scalaCompilerPluginsConfigurationName(mainSourceSet))
    val scalaCompileParameters: Seq[String] = delegate.backend.scalaCompileParameters(projectScalaLibrary.isScala3)

    def configureScalaCompile(sourceSet: SourceSet): Unit =
      val scalaCompile: ScalaCompile = Gradle.scalaCompile(project, sourceSet)
      Gradle.ensureParameters(scalaCompile, scalaCompileParameters)
      Gradle.addScalaCompilerPlugins(scalaCompilerPluginsConfiguration, scalaCompile)

    configureScalaCompile(mainSourceSet)
    configureScalaCompile(testSourceSet)
