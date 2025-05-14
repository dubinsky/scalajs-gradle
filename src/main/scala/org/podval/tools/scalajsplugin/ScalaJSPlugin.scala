package org.podval.tools.scalajsplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.internal.JavaPluginHelper
import org.gradle.api.plugins.jvm.internal.{JvmFeatureInternal, JvmPluginServices}
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.{Plugin, Project}
import org.gradle.jvm.component.internal.DefaultJvmSoftwareComponent
import org.podval.tools.build.{AddConfigurationToClassPath, Gradle, GradleClassPath, ScalaBackendKind, ScalaLibrary, ScalaPlatform}
import org.podval.tools.scalajsplugin.jvm.Jvm
import org.podval.tools.scalajsplugin.scalajs.ScalaJS
import org.podval.tools.scalajsplugin.scalanative.ScalaNative
import org.slf4j.{Logger, LoggerFactory}
import javax.inject.Inject

final class ScalaJSPlugin @Inject(
  jvmPluginServices: JvmPluginServices
) extends Plugin[Project]:
  override def apply(project: Project): Unit =
    project.getPluginManager.apply(classOf[ScalaPlugin])

    val (isModeMixed: Boolean, bindings: Set[BackendDelegateBinding]) = ScalaJSPlugin.getDelegateBindings(project)

    val component: DefaultJvmSoftwareComponent = JavaPluginHelper
      .getJavaComponent(project)
      .asInstanceOf[DefaultJvmSoftwareComponent]

    val sharedFeature: JvmFeatureInternal = component.getMainFeature

    GradleFeatures.configure(
      project,
      component,
      sharedFeature,
      jvmPluginServices,
      isModeMixed,
      bindings
    )

    bindings.foreach((binding: BackendDelegateBinding) =>
      val delegate: BackendDelegate[?] = binding.delegate
      val gradleNames: GradleNames = binding.gradleNames
      
      // Create extension.
      delegate.createExtension.foreach(_.create(project))

      // Set 'runtimeClassPath' and dependency on the 'classes' task.
      project.getTasks.withType(delegate.taskClass).configureEach((task: BackendTask) =>
        def sourceSet: SourceSet = Gradle.getSourceSet(project,
          if task.isTest
          then gradleNames.testSourceSetName
          else gradleNames.mainSourceSetName
        )
        if task.isInstanceOf[BackendTask.HasRuntimeClassPath] then
          task.asInstanceOf[BackendTask.HasRuntimeClassPath].getRuntimeClassPath.setFrom(sourceSet.getRuntimeClasspath)
        if task.isInstanceOf[BackendTask.DependsOnClasses] then
          task.asInstanceOf[BackendTask.DependsOnClasses].dependsOn(Gradle.getClassesTaskProvider(project, sourceSet))
      )
      
      // Create plugin dependencies configuration.
      delegate.pluginDependenciesConfigurationNameOpt.map(configurationName =>
        // TODO use new one-shot methods resolvable()/consumable() etc.
        val configuration: Configuration = project.getConfigurations.create(configurationName)
        configuration.setVisible(false)
        configuration.setCanBeConsumed(false)
        configuration.setDescription(s"${delegate.backendKind.displayName} dependencies used by the ScalaJS plugin.")
      )

      // Tasks.
      delegate.registerTasks(
        project.getTasks,
        linkTaskName = gradleNames.linkTaskName,
        runTaskName = gradleNames.runTaskName,
        testLinkTaskName = gradleNames.testLinkTaskName,
        testTaskName = gradleNames.testSourceSetName // test task and test source set are named the same
      )
    )

    project.afterEvaluate: (project: Project) =>
      val pluginScalaPlatform: ScalaPlatform =
        ScalaLibrary.getFromClasspath(GradleClassPath.collect(this)).toPlatform(ScalaBackendKind.JVM)

      val projectScalaLibrary: ScalaLibrary = ScalaLibrary.getFromConfiguration(
        sharedFeature.getImplementationConfiguration
      )

      bindings.foreach((binding: BackendDelegateBinding) =>
        val gradleNames: GradleNames = binding.gradleNames
        binding.delegate.configure(
          project,
          projectScalaLibrary,
          pluginScalaPlatform,
          mainSourceSetName = gradleNames.mainSourceSetName,
          testSourceSetName = gradleNames.testSourceSetName,
          scalaCompilerPluginsConfigurationName = gradleNames.scalaCompilerPluginsConfigurationName
        )
      )

      // Expand classpath.
      val addToClassPath: Set[AddConfigurationToClassPath] = bindings.flatMap((binding: BackendDelegateBinding) =>
        binding.delegate.pluginDependenciesConfigurationNameOpt.map((pluginDependenciesConfigurationName: String) =>
          AddConfigurationToClassPath(
            project.getConfigurations.getByName(pluginDependenciesConfigurationName),
            project.getConfigurations.getByName(Gradle.getSourceSet(project, binding.gradleNames.mainSourceSetName).getRuntimeClasspathConfigurationName)
          )
        )
      )
      addToClassPath.foreach(_.add())
      addToClassPath.foreach(_.verify(projectScalaLibrary))

object ScalaJSPlugin:
  private val logger: Logger = LoggerFactory.getLogger(ScalaJSPlugin.getClass)

  val backendProperty: String = "org.podval.tools.scalajs.backend"

  private def getDelegateBindings(project: Project): (Boolean, Set[BackendDelegateBinding]) =
    val nonJvmDelegates: Set[BackendDelegate[?]] = Set(ScalaJS, ScalaNative)
      .filter(delegate => project.file(delegate.sourceRoot).exists)

    val delegates: Set[BackendDelegate[?]] =
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
