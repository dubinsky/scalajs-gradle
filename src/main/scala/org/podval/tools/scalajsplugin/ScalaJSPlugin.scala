package org.podval.tools.scalajsplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.tasks.JvmConstants
import org.gradle.api.plugins.{ExtraPropertiesExtension, JavaBasePlugin}
import org.gradle.api.plugins.scala.{ScalaBasePlugin, ScalaPlugin, ScalaPluginExtension}
import org.gradle.api.tasks.{SourceSet, TaskProvider}
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.{Action, Plugin, Project, Task}
import org.gradle.jvm.tasks.Jar
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.podval.tools.build.jvm.JvmBackend
import org.podval.tools.build.{AddConfigurationToClassPath, BackendDependencyRequirements, Dependency, GradleClassPath,
  ScalaBackend, ScalaLibrary, ScalaPlatform}
import org.podval.tools.scalajsplugin.jvm.JvmDelegate
import org.podval.tools.scalajsplugin.scalajs.ScalaJSDelegate
import org.podval.tools.scalajsplugin.scalanative.ScalaNativeDelegate
import org.podval.tools.test.task.{IntelliJIdea, TestTask}
import java.io.File
import ScalaBackend.sharedSourceRoot

final class ScalaJSPlugin extends Plugin[Project]:
  override def apply(project: Project): Unit =
    // Apply Scala plugin to this project.
    project.getPluginManager.apply(classOf[ScalaPlugin])

    def pluginMessage(message: String): String = s"Plugin 'org.podval.tools.scalajs' in $project: $message."

    val delegateOpt: Option[BackendDelegate[?]] = Option(project.findProperty(ScalaJSPlugin.backendProperty))
      .map(_.toString)
      .map((name: String) => Set(JvmDelegate, ScalaJSDelegate, ScalaNativeDelegate)
        .find(_.backend.name == name)
        .getOrElse(throw IllegalArgumentException(pluginMessage(
          s"unknown Scala backend '$name'; known backends: ${ScalaBackend.all.map(_.name).mkString(", ")}"
        )))
      )

    def isBackendPresent(backend: ScalaBackend): Boolean = project.file(backend.sourceRoot).exists && {
      val isSubproject: Boolean = Gradle.findSubproject(project, backend.sourceRoot).isDefined
      if !isSubproject then project.getLogger.warn(pluginMessage(
        s"'${backend.sourceRoot}' is not included as a subproject in 'settings.gradle'."
      ))
      isSubproject
    }

    val subprojectBackends: Set[ScalaBackend] = ScalaBackend.all.filter(isBackendPresent)

    if delegateOpt.isEmpty && subprojectBackends.isEmpty then project.getLogger.lifecycle(pluginMessage(
      s"""to choose Scala backend, set property '${ScalaJSPlugin.backendProperty}' to one of ${ScalaBackend.all.map(_.name).mkString(", ")}
         |or include in 'settings.gradle' at least one of the subprojects ${ScalaBackend.all.map(_.sourceRoot).mkString(", ")};
         |see documentation at: https://github.com/dubinsky/scalajs-gradle""".stripMargin
    ))

    val isRunningInIntelliJ: Boolean = IntelliJIdea.runningIn

    val sharedExists: Boolean =
      val file: File = project.file(sharedSourceRoot)
      file.exists && file.isDirectory
    
    if delegateOpt.isEmpty && subprojectBackends.nonEmpty && !sharedExists then project.getLogger.lifecycle(pluginMessage(
      s"to share code between backends, create directory '$sharedSourceRoot'"
    ))
    
    val sharedNotAvailable: Boolean =
      sharedExists && 
      isRunningInIntelliJ &&
      Option(project.findProject(sharedSourceRoot)).isEmpty
        
    if sharedNotAvailable then project.getLogger.warn(pluginMessage(
      s"for shared sources to be visible in IntelliJ IDE, '$sharedSourceRoot' must be included as a subproject in 'settings.gradle'"
    ))
      
    if delegateOpt.isEmpty && subprojectBackends.nonEmpty then
      project.getLogger.lifecycle(pluginMessage(s"using Scala backends ${subprojectBackends.map(_.name).mkString(", ")}"))
      ScalaJSPlugin.applyMixed(
        project,
        subprojectBackends,
        includeShared = sharedExists && !sharedNotAvailable
      )
    else
      val includeShared: Boolean = Option(project.findProperty(ScalaJSPlugin.includeSharedProperty))
        .map(_.toString)
        .contains("true")

      val sharedSiblingOpt: Option[Project] =
        if !includeShared
        then None
        else Option(project.getParent.findProject(sharedSourceRoot))

      val delegate: BackendDelegate[?] = delegateOpt.getOrElse(JvmDelegate)
      val sharedString: String = if !includeShared then "" else s" (including '$sharedSourceRoot')"
      val ijString: String = if !isRunningInIntelliJ then "" else " [IJ]"
      project.getLogger.lifecycle(pluginMessage(s"using Scala backend ${delegate.backend.name}$sharedString$ijString"))

      ScalaJSPlugin.applySingle(
        project,
        delegate,
        sharedSiblingOpt,
        includeShared = includeShared,
        isRunningInIntelliJ = isRunningInIntelliJ
      )

object ScalaJSPlugin:
  val backendProperty: String = "org.podval.tools.scalajs.backend"
  val includeSharedProperty: String = "org.podval.tools.scalajs.includeShared"

  private def applyMixed(
    project: Project,
    subprojectBackends: Set[ScalaBackend],
    includeShared: Boolean
  ): Unit =
    val sharedSubprojectOpt: Option[Project] = Gradle.findSubproject(project, sharedSourceRoot)

    // Disable all tasks and remove all Scala sources from the overall project.
    Gradle.disableAllTasks(project)
    Gradle.removeAllScalaSources(project)

    // Disable all tasks and apply 'scala' plugin to the shared project (if it exists).
    sharedSubprojectOpt.foreach((sharedSubproject: Project) =>
      Gradle.disableAllTasks(sharedSubproject)
      sharedSubproject.getPluginManager.apply("scala")
    )

    for subprojectBackend: ScalaBackend <- subprojectBackends do
      // Configure backend and apply the plugin to the subprojects.
      val subproject: Project = Gradle.findSubproject(project, subprojectBackend.sourceRoot).get
      val backendName: String = subprojectBackend.name
      val extraProperties: ExtraPropertiesExtension = subproject.getExtensions.getByType(classOf[ExtraPropertiesExtension])
      extraProperties.set(backendProperty, backendName)
      if includeShared then extraProperties.set(includeSharedProperty, "true")
      subproject.getPluginManager.apply(classOf[ScalaJSPlugin])

    project.afterEvaluate: (project: Project) =>
      // Set Scala version on the subprojects and the shared project (if it exists).
      val scalaLibraryDependency: Dependency.WithVersion = ScalaLibrary.getFromProject(project).dependencyWithVersion
      val subprojectsToSetScalaVersionOn: Seq[Project] =
        sharedSubprojectOpt.toSeq ++
        subprojectBackends.map(_.sourceRoot).map(Gradle.findSubproject(project, _)).map(_.get)
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
    sharedSiblingOpt: Option[Project],
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
        sharedSiblingOpt.foreach(sharedSibling => project
          .getDependencies
          .add(JvmConstants.IMPLEMENTATION_CONFIGURATION_NAME, sharedSibling)
        )

        val addSharedScalaSourcesForTask: Action[Task] = Gradle.addSharedScalaSourcesForTask(project)
        project.getTasks.withType(classOf[ScalaCompile]).configureEach(addSharedScalaSourcesForTask)
        project.getTasks.withType(classOf[Jar]).configureEach(addSharedScalaSourcesForTask)

    project.afterEvaluate: (project: Project) =>
      val projectScalaLibrary: ScalaLibrary = ScalaLibrary.getFromProject(project)
      val pluginScalaPlatform: ScalaPlatform = ScalaLibrary.getFromClasspath(GradleClassPath.collect(this)).toPlatform(JvmBackend)
      configureJarTask(project, delegate, projectScalaLibrary)
      applyDependencyRequirements(project, delegate, projectScalaLibrary, pluginScalaPlatform)
      configureScalaCompile(project, delegate, projectScalaLibrary)
      expandClassPath(project, delegate, projectScalaLibrary)

  private def configureArchiveAppendix(
    project: Project,
    delegate: BackendDelegate[?]
  ): Unit =
    delegate.backend.archiveAppendixOpt.foreach((archiveAppendix: String) =>
      project.getTasks.withType(classOf[Jar])
        .configureEach(Gradle.archiveAppendixConvention(archiveAppendix, project))
    )

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
  ): Unit =
    delegate.pluginDependenciesConfigurationNameOpt.map(configurationName =>
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

  private def applyDependencyRequirements(
    project: Project,
    delegate: BackendDelegate[?],
    projectScalaLibrary: ScalaLibrary,
    pluginScalaPlatform: ScalaPlatform
  ): Unit =
    val projectScalaPlatform: ScalaPlatform = projectScalaLibrary.toPlatform(delegate.backend)

    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = Gradle.getSourceSets(project)
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
        ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME
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
    val scalaCompilerPluginsConfiguration: Configuration =
      project.getConfigurations.getByName(ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME)
    val scalaCompileParameters: Seq[String] = delegate.backend.scalaCompileParameters(projectScalaLibrary.isScala3)

    def configureScalaCompile(sourceSet: SourceSet): Unit =
      val scalaCompile: ScalaCompile = Gradle.scalaCompile(project, sourceSet)
      Gradle.ensureParameters(scalaCompile, scalaCompileParameters)
      Gradle.addScalaCompilerPlugins(scalaCompilerPluginsConfiguration, scalaCompile)

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
