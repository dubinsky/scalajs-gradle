package org.podval.tools.scalajsplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.tasks.JvmConstants
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.{ExtraPropertiesExtension, JavaBasePlugin}
import org.gradle.api.plugins.scala.{ScalaBasePlugin, ScalaPlugin, ScalaPluginExtension}
import org.gradle.api.tasks.{SourceSet, SourceTask, TaskProvider}
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.{Action, GradleException, Plugin, Project, Task}
import org.gradle.jvm.tasks.Jar
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.podval.tools.build.jvm.JvmBackend
import org.podval.tools.build.{AddConfigurationToClassPath, BackendDependencyRequirements, Dependency, GradleClassPath,
  ScalaBackend, ScalaLibrary, ScalaPlatform}
import org.podval.tools.build.ScalaBackend.sharedSourceRoot
import org.podval.tools.platform.IntelliJIdea
import org.podval.tools.scalajsplugin.jvm.JvmDelegate
import org.podval.tools.scalajsplugin.scalajs.ScalaJSDelegate
import org.podval.tools.scalajsplugin.scalanative.ScalaNativeDelegate
import org.podval.tools.test.task.TestTask
import org.podval.tools.util.Files
import org.slf4j.Logger
import scala.jdk.CollectionConverters.*
import java.io.File

final class ScalaJSPlugin extends Plugin[Project]:
  override def apply(project: Project): Unit =
    // Apply Scala plugin to this project.
    project.getPluginManager.apply(classOf[ScalaPlugin])

    def backendNames: String = ScalaBackend.all.map(backend => s"${backend.name} (${backend.sourceRoot})").mkString(", ")
    def sourceRoots: String = ScalaBackend.all.map(_.sourceRoot).mkString(", ")
    def pluginMessage(message: String): String = s"Plugin 'org.podval.tools.scalajs' in $project: $message."
    def help(message: String): Unit = project.getLogger.lifecycle(
      s"${pluginMessage(message)}\nDocumentation: https://github.com/dubinsky/scalajs-gradle"
    )

    val delegateOpt: Option[BackendDelegate[?]] = Option(project.findProperty(ScalaJSPlugin.backendProperty))
      .map(_.toString)
      .map((name: String) => Set(JvmDelegate, ScalaJSDelegate, ScalaNativeDelegate)
        .find(_.backend.is(name))
        .getOrElse(throw GradleException(pluginMessage(s"unknown Scala backend '$name'; use one of $backendNames")))
      )
    
    val backendCandidates: Set[ScalaBackend] = ScalaBackend.all.filter(backend => project.file(backend.sourceRoot).exists)
    val notSubprojects: Set[ScalaBackend] = backendCandidates.filterNot(ScalaJSPlugin.findBackendSubproject(project, _).isDefined)
    if notSubprojects.nonEmpty then project.getLogger.warn(pluginMessage(
        s"subprojects ${notSubprojects.map(_.sourceRoot).map(n => s"'$n'").mkString(", ")} must be included in 'settings.gradle'"
    ))
    
    if delegateOpt.isEmpty && backendCandidates.isEmpty then help(
      s"""to choose Scala backend, set property '${ScalaJSPlugin.backendProperty}' to one of $backendNames;
         |to share code between backends, create at least one of the subprojects $sourceRoots""".stripMargin
    )

    val isModeMixed: Boolean = delegateOpt.isEmpty && backendCandidates.nonEmpty
    
    // Write `settings-includes.gradle`.
    if isModeMixed then Files.write(
      file = File(project.getProjectDir, "settings-includes.gradle"),
      content = (Seq(sharedSourceRoot) ++ backendCandidates.map(_.sourceRoot))
        .map(name => s"include '${project.getProjectDir.getName}:$name'")
        .mkString("\n")
    )

    val sharedExists: Boolean =
      val file: File = project.file(sharedSourceRoot)
      file.exists && file.isDirectory

    if sharedExists then
      if !isModeMixed then help(
        s"""to share code between backends, do not set property '${ScalaJSPlugin.backendProperty}'
           |and create at least one of the subprojects $sourceRoots""".stripMargin
      )
    else
      if isModeMixed then help(
        s"to share code between backends, create directory '$sharedSourceRoot'"
      )

    val isRunningInIntelliJ: Boolean = IntelliJIdea.runningIn
    val sharedNotAvailable: Boolean = sharedExists && isRunningInIntelliJ && ScalaJSPlugin.findSharedSubproject(project).isEmpty

    if sharedNotAvailable then help(
      s"for shared sources to be visible in IntelliJ IDE, include subproject '$sharedSourceRoot' in 'settings.gradle'"
    )

    val ijString: String = if !isRunningInIntelliJ then "" else " [IJ]"
    if isModeMixed then
      val backends: Set[ScalaBackend] = backendCandidates.filter(ScalaJSPlugin.findBackendSubproject(project, _).isDefined)
      project.getLogger.lifecycle(pluginMessage(s"using Scala backends ${backends.map(_.name).mkString(", ")}$ijString"))
      ScalaJSPlugin.applyMixed(
        project,
        backends,
        includeShared = sharedExists && !sharedNotAvailable
      )
    else
      val includeShared: Boolean = Option(project.findProperty(ScalaJSPlugin.includeSharedProperty))
        .map(_.toString)
        .contains("true")

      val delegate: BackendDelegate[?] = delegateOpt.getOrElse(JvmDelegate)
      val sharedString: String = if !includeShared then "" else s" (including '$sharedSourceRoot')"
      project.getLogger.lifecycle(pluginMessage(s"using Scala backend ${delegate.backend.name}$sharedString$ijString"))

      ScalaJSPlugin.applySingle(
        project,
        delegate,
        includeShared = includeShared,
        isRunningInIntelliJ = isRunningInIntelliJ
      )

object ScalaJSPlugin:
  val backendProperty: String = "org.podval.tools.scalajs.backend"
  val includeSharedProperty: String = "org.podval.tools.scalajs.includeShared"

  // We look up projects by their *directory* names, not by their *project* names,
  // so `Option(project.findProject(name))` does not do it for projects renamed in `settings.gradle` ;)
  private def findSubproject(project: Project, name: String): Option[Project] = project
    .getSubprojects
    .asScala
    .find(_.getProjectDir.getName == name)

  private def findSharedSubproject(project: Project) = findSubproject(project, sharedSourceRoot)
  private def findBackendSubproject(project: Project, backend: ScalaBackend) = findSubproject(project, backend.sourceRoot)

  private def applyMixed(
    project: Project,
    backends: Set[ScalaBackend],
    includeShared: Boolean
  ): Unit =
    val sharedSubprojectOpt: Option[Project] = findSharedSubproject(project)

    // Disable `SourceTask` tasks of the overall project and unregister all its Scala sources.
    project.getTasks.withType(classOf[SourceTask]).configureEach(_.setEnabled(false))
    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = Gradle.getSourceSets(project)
    Gradle.getScalaSourceDirectorySet(mainSourceSet).setSrcDirs(List.empty.asJava)
    Gradle.getScalaSourceDirectorySet(testSourceSet).setSrcDirs(List.empty.asJava)

    // If the shared subproject exists, apply 'scala' plugin to it and disable all its tasks.
    sharedSubprojectOpt.foreach((sharedSubproject: Project) =>
      sharedSubproject.getPluginManager.apply("scala")
      sharedSubproject.getTasks.withType(classOf[Task]).configureEach(_.setEnabled(false))
    )

    for backend: ScalaBackend <- backends do
      // Configure backend and apply the plugin to the subprojects.
      val subproject: Project = findBackendSubproject(project, backend).get
      val extraProperties: ExtraPropertiesExtension = subproject.getExtensions.getByType(classOf[ExtraPropertiesExtension])
      extraProperties.set(backendProperty, backend.name)
      if includeShared then extraProperties.set(includeSharedProperty, "true")
      subproject.getPluginManager.apply(classOf[ScalaJSPlugin])

    project.afterEvaluate: (project: Project) =>
      // Set Scala version on the subprojects and the shared project (if it exists).
      val subprojectsToSetScalaVersionOn: Seq[Project] =
        sharedSubprojectOpt.toSeq ++
        backends.map(findBackendSubproject(project, _)).map(_.get)

      val scalaLibraryDependency: Dependency.WithVersion = ScalaLibrary.getFromProject(project).dependencyWithVersion
      subprojectsToSetScalaVersionOn.foreach(_
        .getExtensions
        .getByType(classOf[ScalaPluginExtension])
        .getScalaVersion
        .set(scalaLibraryDependency.version.toString)
// Using implementation dependency:
//        _.getDependencies.add(
//          JvmConstants.IMPLEMENTATION_CONFIGURATION_NAME,
//          scalaLibraryDependency.dependencyNotation
//        )
      )

  private def applySingle(
    project: Project,
    delegate: BackendDelegate[?],
    includeShared: Boolean,
    isRunningInIntelliJ: Boolean
  ): Unit =
    configureArchiveAppendix(project, delegate)
    configureTestTask(project)
    configureRuntimeAndClassesTasks(project, delegate)
    delegate.createExtension.foreach(_.create(project))
    createPluginDependenciesConfiguration(project, delegate)
    registerTasks(project, delegate)

    if includeShared then
      if !isRunningInIntelliJ then
        Gradle.addSharedScalaSources(project)
      else
        // Add dependency on the shared sibling.
        // TODO exclude this dependency from publications!
        val sharedSibling: Project = ScalaJSPlugin.findSharedSubproject(project.getParent).get
        project.getDependencies.add(JvmConstants.IMPLEMENTATION_CONFIGURATION_NAME, sharedSibling)

        // Add shared sources for the execution of the tasks that need them.
        val addSharedScalaSourcesForTask: Action[Task] = Gradle.addSharedScalaSourcesForTask(project)
        project.getTasks.withType(classOf[ScalaCompile]).configureEach(addSharedScalaSourcesForTask)
        project.getTasks.withType(classOf[Jar         ]).configureEach(addSharedScalaSourcesForTask)

    project.afterEvaluate: (project: Project) =>
      val projectScalaLibrary: ScalaLibrary = ScalaLibrary.getFromProject(project)
      val pluginScalaPlatform: ScalaPlatform = ScalaLibrary.getFromClasspath(GradleClassPath.collect(this)).toPlatform(JvmBackend)
      configureJarTask(project, delegate, projectScalaLibrary)
      dependencyRequirements(project, delegate, projectScalaLibrary, pluginScalaPlatform).foreach(_.apply(project))
      configureScalaCompile(project, delegate, projectScalaLibrary)
      expandClassPath(project, delegate, projectScalaLibrary)

  private def configureArchiveAppendix(
    project: Project,
    delegate: BackendDelegate[?]
  ): Unit = delegate.backend.archiveAppendixOpt.foreach((archiveAppendix: String) =>
    project.getTasks.withType(classOf[Jar])
      .configureEach(Gradle.archiveAppendixConvention(archiveAppendix, project))
  )

  private def configureTestTask(
    project: Project
  ): Unit = project.getTasks.withType(classOf[TestTask]).configureEach((testTask: TestTask) =>
    testTask.setGroup(JavaBasePlugin.VERIFICATION_GROUP)
    testTask.useSbt()
  )

  // Set 'runtimeClassPath' and dependency on the 'classes' task.
  private def configureRuntimeAndClassesTasks(
    project: Project,
    delegate: BackendDelegate[?]
  ): Unit = project.getTasks.withType(delegate.taskClass).configureEach((task: BackendTask) =>
    def sourceSet: SourceSet =
      val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = Gradle.getSourceSets(project)
      if task.isTest
      then testSourceSet
      else mainSourceSet

    if task.isInstanceOf[BackendTask.HasRuntimeClassPath] then
      task.asInstanceOf[BackendTask.HasRuntimeClassPath].getRuntimeClassPath.setFrom(sourceSet.getRuntimeClasspath)
    if task.isInstanceOf[BackendTask.DependsOnClasses] then
      task.asInstanceOf[BackendTask.DependsOnClasses].dependsOn(Gradle.getClassesTaskProvider(project, sourceSet))
  )

  private def createPluginDependenciesConfiguration(
    project: Project,
    delegate: BackendDelegate[?]
  ): Unit = delegate.pluginDependenciesConfigurationNameOpt.map((configurationName: String) =>
    // TODO use new one-shot methods resolvable()/consumable() etc.
    val configuration: Configuration = project.getConfigurations.create(configurationName)
    configuration.setVisible(false)
    configuration.setCanBeConsumed(false)
    configuration.setDescription(s"${delegate.backend.name} dependencies used by the ScalaJS plugin.")
  )

  private def registerTasks(
    project: Project,
    delegate: BackendDelegate[?]
  ): Unit =
    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = Gradle.getSourceSets(project)
    def linkTaskName(sourceSet: SourceSet): String = sourceSet.getTaskName("link", "")

    // TODO look into link tasks self-registering run/test counterparts.
    
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
        task.setDescription(s"$before ${delegate.backend.name} code$after.")
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
      name = "run",
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
      group = LifecycleBasePlugin.VERIFICATION_GROUP,
      dependsOn = linkTest,
      replace = true
    )

  private def configureJarTask(
    project: Project,
    delegate: BackendDelegate[?],
    projectScalaLibrary: ScalaLibrary
  ) : Unit =
    val jarTaskName: String = JvmConstants.JAR_TASK_NAME
    project.getTasks.withType(classOf[Jar]).named(jarTaskName).configure(
      Gradle.removeDashBeforeArchiveAppendix(project)
    )
    val jarAppendix: String = delegate.backend.artifactSuffixString + projectScalaLibrary.suffixString
    project.getTasks.withType(classOf[Jar]).named(jarTaskName).configure((jar: Jar) =>
      jar.getArchiveAppendix.convention(jarAppendix)
    )

  private def dependencyRequirements(
    project: Project,
    delegate: BackendDelegate[?],
    projectScalaLibrary: ScalaLibrary,
    pluginScalaPlatform: ScalaPlatform
  ): Seq[ApplyDependencyRequirements] =
    val projectScalaPlatform: ScalaPlatform = projectScalaLibrary.toPlatform(delegate.backend)
    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = Gradle.getSourceSets(project)
    val implementationConfigurationName    : String = mainSourceSet.getImplementationConfigurationName
    val testImplementationConfigurationName: String = testSourceSet.getImplementationConfigurationName

    val requirements: BackendDependencyRequirements = delegate.backend.dependencyRequirements(
      implementationConfiguration = project.getConfigurations.getByName(implementationConfigurationName),
      testImplementationConfiguration = project.getConfigurations.getByName(testImplementationConfigurationName),
      projectScalaPlatform = projectScalaPlatform
    )

    Seq(
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
        ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME
      )
    ) ++ delegate.pluginDependenciesConfigurationNameOpt.toSeq.map((pluginDependenciesConfigurationName: String) =>
      ApplyDependencyRequirements(
        requirements.pluginDependencies,
        pluginScalaPlatform,
        pluginDependenciesConfigurationName
      )
    )

  private def configureScalaCompile(
    project: Project,
    delegate: BackendDelegate[?],
    projectScalaLibrary: ScalaLibrary
  ): Unit =
    val scalaCompilerPluginsConfiguration: Configuration =
      project.getConfigurations.getByName(ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME)
    val scalaCompileParameters: Seq[String] = delegate.backend.scalaCompileParameters(projectScalaLibrary.isScala3)

    def configureScalaCompile(sourceSet: SourceSet): Unit =
      val scalaCompile: ScalaCompile = Gradle.scalaCompile(project, sourceSet)
      ensureParameters(scalaCompile, scalaCompileParameters, project.getLogger)
      addScalaCompilerPlugins(scalaCompilerPluginsConfiguration, scalaCompile, project.getLogger)

    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = Gradle.getSourceSets(project)
    configureScalaCompile(mainSourceSet)
    configureScalaCompile(testSourceSet)

  private def expandClassPath(
    project: Project,
    delegate: BackendDelegate[?],
    projectScalaLibrary: ScalaLibrary
  ): Unit =
    val addToClassPath: Option[AddConfigurationToClassPath] = delegate
      .pluginDependenciesConfigurationNameOpt
      .map((pluginDependenciesConfigurationName: String) =>
        AddConfigurationToClassPath(
          project.getConfigurations.getByName(pluginDependenciesConfigurationName),
          project.getConfigurations.getByName(JvmConstants.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        )
      )

    addToClassPath.foreach(_.add())
    addToClassPath.foreach(_.verify(projectScalaLibrary))

  private def ensureParameters(
    scalaCompile: ScalaCompile,
    toAdd: Seq[String],
    logger: Logger
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
    scalaCompile: ScalaCompile,
    logger: Logger
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
