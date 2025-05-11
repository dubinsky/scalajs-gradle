package org.podval.tools.scalajsplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.{Plugin, Project, Task}
import org.podval.tools.build.{Gradle, GradleClassPath, ScalaBackendKind, ScalaLibrary, ScalaPlatform}
import org.podval.tools.scalajsplugin.jvm.Jvm
import org.podval.tools.scalajsplugin.scalajs.ScalaJS
import org.podval.tools.scalajsplugin.scalanative.ScalaNative
import org.podval.tools.test.task.TestTask
import org.slf4j.{Logger, LoggerFactory}
import java.io.File
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala, SeqHasAsJava}
import javax.inject.Inject

// TODO add dependency on the backend test task to the overall test task
final class ScalaJSPlugin @Inject(
  jvmPluginServices: JvmPluginServices
) extends Plugin[Project]:
  override def apply(project: Project): Unit =
    project.getPluginManager.apply(classOf[org.gradle.api.plugins.scala.ScalaPlugin])

    val (isModeMixed: Boolean, bindings: Set[BackendDelegateBinding]) = ScalaJSPlugin.getDelegateBindings(project)

    val sharedImplementationConfigurationName: String = GradleFeatures.configure(
      project,
      jvmPluginServices,
      isModeMixed,
      bindings
    )

    for binding: BackendDelegateBinding <- bindings do
      binding.delegate.pluginDependenciesConfigurationNameOpt.map(configurationName =>
        // TODO use new one-shot methods resolvable()/consumable() etc.
        val configuration: Configuration = project.getConfigurations.create(configurationName)
        configuration.setVisible(false)
        configuration.setCanBeConsumed(false)
        configuration.setDescription(s"${binding.delegate.backendKind.displayName} dependencies used by the ScalaJS plugin.")
      )

      binding.delegate.createExtensions(project)

      val testTaskDependency: Option[TaskProvider[? <: Task]] = binding.delegate.addTasks(project, binding.gradleNames)

      val testTask: TestTask = project.getTasks.replace(binding.gradleNames.testSourceSetName, binding.delegate.testTaskClass)
      testTask.dependsOn(Gradle.getClassesTaskProvider(project, binding.gradleNames.testSourceSetName))
      testTaskDependency.foreach(testTask.dependsOn(_))

      project.getTasks.withType(binding.delegate.testTaskClass).configureEach((task: TestTask) =>
        task.setDescription(s"Test ${binding.delegate.backendKind.displayName} code using sbt frameworks.")
      )
    
    project.afterEvaluate: (project: Project) =>
      val pluginScalaPlatform: ScalaPlatform =
        ScalaLibrary.getFromClasspath(GradleClassPath.collect(this)).toPlatform(ScalaBackendKind.JVM)

      val addToClassPath: Set[Option[AddToClassPath]] = for binding: BackendDelegateBinding <- bindings yield
        val projectScalaLibrary: ScalaLibrary = ScalaLibrary.getFromConfiguration(
          project,
          sharedImplementationConfigurationName
       )

        val projectScalaPlatform: ScalaPlatform = projectScalaLibrary.toPlatform(binding.delegate.backendKind)
        val isScala3: Boolean = projectScalaPlatform.version.isScala3

        binding.delegate.applyDependencyRequirements(
          project,
          binding.gradleNames,
          pluginScalaPlatform,
          projectScalaPlatform,
          isScala3
        )
        
        val toAdd: Seq[String] = binding.delegate.scalaCompileParameters(isScala3)

        def configureScalaCompile(
          sourceSetName: String,
        ): Unit =
          val scalaCompile: ScalaCompile = Gradle.getScalaCompile(project, sourceSetName)

          ScalaJSPlugin.ensureParameters(
            scalaCompile,
            sourceSetName,
            toAdd
          )

          ScalaJSPlugin.addScalaCompilerPlugins(
            project,
            scalaCompile,
            binding.gradleNames.scalaCompilerPluginsConfigurationName
          )
        
        configureScalaCompile(binding.gradleNames.mainSourceSetName)
        configureScalaCompile(binding.gradleNames.testSourceSetName)

        binding.delegate.pluginDependenciesConfigurationNameOpt.map(configurationName => AddToClassPath(
          configurationName,
          projectScalaLibrary,
          Gradle.getSourceSet(project, binding.gradleNames.mainSourceSetName).getRuntimeClasspathConfigurationName
        ))

      // Expand classpath.
      addToClassPath.flatten.foreach(_.add(project))
      addToClassPath.flatten.foreach(_.verify(project))

object ScalaJSPlugin:
  private val logger: Logger = LoggerFactory.getLogger(ScalaJSPlugin.getClass)

  val backendProperty: String = "org.podval.tools.scalajs.backend"

  private def getDelegateBindings(project: Project): (Boolean, Set[BackendDelegateBinding]) =
    val nonJvmDelegates: Set[BackendDelegate] = Set(ScalaJS, ScalaNative)
      .filter(delegate => project.file(delegate.sourceRoot).exists)

    val delegates: Set[BackendDelegate] =
      if nonJvmDelegates.nonEmpty
      then nonJvmDelegates.incl(Jvm)
      else Set(Option(project.findProperty(backendProperty)).map(_.toString) match
        case None => Jvm
        case Some(name) => Set(Jvm, ScalaJS, ScalaNative)
          .find(_.backendKind.name == name)
          .getOrElse(throw IllegalArgumentException(s"Unknown backend '$name'."))
      )
    
    val delegateNames: String = delegates.map(_.backendKind.displayName).mkString(",")
    logger.info(s"ScalaJSPlugin: running with $delegateNames.")
    nonJvmDelegates.nonEmpty -> delegates.map(_.bind(nonJvmDelegates.nonEmpty))
  
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
          logger.info(s"scalaCompileOptions.additionalParameters of the ${scalaCompile.getName} task: adding '$parameter'.")
          parameters :+ parameter
    }
  
    scalaCompile
      .getScalaCompileOptions
      .setAdditionalParameters(parametersNew.asJava)
  
  private def addScalaCompilerPlugins(
    project: Project,
    scalaCompile: ScalaCompile,
    scalaCompilerPluginsConfigurationName: String
  ): Unit =
    // There seems to be no need to add `"-Xplugin:" + plugin.getPath` parameters:
    // just adding plugins to the list is sufficient.
    val scalaCompilerPluginsConfiguration: Configuration = Gradle.getConfiguration(
      project,
      scalaCompilerPluginsConfigurationName
    )
    val scalaCompilerPlugins: Iterable[File] = scalaCompilerPluginsConfiguration.asScala
    if scalaCompilerPlugins.nonEmpty then
      logger.info(s"Adding ${scalaCompile.getName} to $scalaCompilerPluginsConfigurationName: $scalaCompilerPlugins.")
      val plugins: FileCollection = Option(scalaCompile.getScalaCompilerPlugins)
        .map((existingPlugins: FileCollection) => existingPlugins.plus(scalaCompilerPluginsConfiguration))
        .getOrElse(scalaCompilerPluginsConfiguration)
      scalaCompile.setScalaCompilerPlugins(plugins)
