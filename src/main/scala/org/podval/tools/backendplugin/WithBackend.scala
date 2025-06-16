package org.podval.tools.backendplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.{Action, Project, Task}
import org.gradle.api.file.{DirectoryProperty, FileCollection}
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.tasks.JvmConstants
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.{SourceSet, TaskProvider}
import org.gradle.jvm.tasks.Jar
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.util.internal.GUtil
import org.podval.tools.backendplugin.jvm.JvmDelegate
import org.podval.tools.backendplugin.scalajs.ScalaJSDelegate
import org.podval.tools.backendplugin.scalanative.ScalaNativeDelegate
import org.podval.tools.build.{ClassPathAddition, DependencyRequirements, BackendDependencyRequirements,
  ClassPathAdditions, GradleClassPath, ScalaBackend, ScalaLibrary, ScalaVersion}
import org.podval.tools.test.task.TestTask
import org.slf4j.Logger
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala, SeqHasAsJava}
import java.io.File

final class WithBackend(
  project: Project,
  extension: BackendExtension,
  backend: ScalaBackend,
  shared: Option[Project],
  jvmPluginServices: JvmPluginServices
):
  private def scalaCompilerPluginsConfigurationName: String = ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME
  private def testScalaCompilerPluginsConfigurationName: String = GUtil.toLowerCamelCase(s"test $scalaCompilerPluginsConfigurationName")
  private def getSourceSet(isTest: Boolean): SourceSet = Sources.getSourceSet(project, isTest)

  def apply(): Unit =
    val delegate: BackendDelegate[?] = Set(JvmDelegate, ScalaJSDelegate, ScalaNativeDelegate)
      .find(_.backend == backend)
      .get

    val pluginDependenciesConfigurationName: Option[String] = delegate.pluginDependenciesConfigurationNameOpt

    BackendPlugin.setScalaVersionFromParentAndAddVersionSpecificScalaSources(project, extension)
    project.afterEvaluate((_: Project) => afterEvaluate(pluginDependenciesConfigurationName))

    extension.setBackend(backend)

    createTestScalaCompilerPluginsConfiguration()
    configureTestTask()
    configureHasRuntimeClassPathTasks()
    configureDependsOnClassesTasks()

    backend.archiveAppendixOpt.foreach(configureArchiveAppendix)
    delegate.createExtension.foreach(_.apply(project))
    pluginDependenciesConfigurationName.foreach(createPluginDependenciesConfiguration(_, backend.name))
    registerTasks(delegate)
  
  private def afterEvaluate(pluginDependenciesConfigurationName: Option[String]): Unit =
    shared.foreach((shared: Project) => Sources.addSharedSources(
      project,
      shared,
      extension.isRunningInIntelliJ
    ))

    val scalaVersion: ScalaVersion = extension.getScalaVersion

    configureJarTask(scalaVersion)

    // Adjust the build directory for the Scala version if requested.
    if extension.isBuildPerScalaVersion then
      val buildDirectory: DirectoryProperty = project.getLayout.getBuildDirectory
      buildDirectory.set(buildDirectory.get.dir(s"scala-$scalaVersion"))
    
    dependencyRequirements(
      scalaVersion,
      pluginScalaVersion = ScalaLibrary.getFromClasspath(GradleClassPath.collect(this)).scalaVersion,
      pluginDependenciesConfigurationName
    ).foreach(_.apply(project))

    configureScalaCompile(scalaVersion)

    ClassPathAdditions(pluginDependenciesConfigurationName
      .toSeq
      .map(ClassPathAddition(
        _,
        getSourceSet(isTest = false).getRuntimeClasspathConfigurationName
      ))
    )
      .apply(
        project,
        extension.getScalaLibrary
      )

  private def configureArchiveAppendix(archiveAppendix: String): Unit =
    val archiveAppendixConvention: Action[Jar] = (jar: Jar) => jar
      .getArchiveAppendix
      .convention(project.provider(() => if jar.getArchiveClassifier.isPresent then archiveAppendix else null))

    project
      .getTasks
      .withType(classOf[Jar])
      .configureEach(archiveAppendixConvention)

  private def configureJarTask(scalaVersion: ScalaVersion): Unit =
    val jarTaskName: String = JvmConstants.JAR_TASK_NAME
    project.getTasks.withType(classOf[Jar]).named(jarTaskName).configure(
      removeDashBeforeArchiveAppendix()
    )

    val jarAppendix: String = s"${backend.artifactSuffixString}_${scalaVersion.binaryVersion.versionSuffix}"
    project.getTasks.withType(classOf[Jar]).named(jarTaskName).configure((jar: Jar) =>
      jar.getArchiveAppendix.convention(jarAppendix)
    )

  private def removeDashBeforeArchiveAppendix(): Action[Jar] = (jar: Jar) => jar
    .getArchiveFileName
    .convention(project.provider(() =>
      // The only change: no dash before the appendix.
      // [baseName][appendix]-[version]-[classifier].[extension]
      var name: String = GUtil.elvis(jar.getArchiveBaseName.getOrNull, "")
      name += GUtil.elvis(jar.getArchiveAppendix.getOrNull, "")
      name += maybe(name, jar.getArchiveVersion.getOrNull)
      name += maybe(name, jar.getArchiveClassifier.getOrNull)

      val extension: String = jar.getArchiveExtension.getOrNull
      name += (if GUtil.isTrue(extension) then "." + extension else "")
      name
    ))

  private def maybe(prefix: String, value: String): String =
    if !GUtil.isTrue(value) then ""
    else if !GUtil.isTrue(prefix) then value
    else "-".concat(value)

  private def configureTestTask(): Unit = project
    .getTasks
    .withType(classOf[TestTask])
    .configureEach((testTask: TestTask) =>
      testTask.setGroup(JavaBasePlugin.VERIFICATION_GROUP)
      testTask.useSbt()
    )

  private def configureHasRuntimeClassPathTasks(): Unit = project
    .getTasks
    .withType(classOf[BackendTask.HasRuntimeClassPath])
    .configureEach(task => task.getRuntimeClassPath.setFrom(getSourceSet(task.isTest).getRuntimeClasspath))

  private def configureDependsOnClassesTasks(): Unit = project
    .getTasks
    .withType(classOf[BackendTask.DependsOnClasses])
    .configureEach(task => task.dependsOn(project.getTasks.named(getSourceSet(task.isTest).getClassesTaskName)))

  private def createPluginDependenciesConfiguration(
    configurationName: String,
    backendName: String
  ): Unit =
    // TODO use new one-shot methods resolvable()/consumable() etc.
    val configuration: Configuration = project.getConfigurations.create(configurationName)
    configuration.setVisible(false)
    configuration.setCanBeConsumed(false)
    configuration.setDescription(s"$backendName dependencies used by the ScalaJS plugin.")

  private def createTestScalaCompilerPluginsConfiguration(): Unit =
    val testPlugins: Configuration = project
      .asInstanceOf[ProjectInternal]
      .getConfigurations
      .resolvableDependencyScopeUnlocked(testScalaCompilerPluginsConfigurationName)
    testPlugins.setTransitive(false)
    jvmPluginServices.configureAsRuntimeClasspath(testPlugins)

  private def registerTasks(delegate: BackendDelegate[?]): Unit =
    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = Sources.getSourceSets(project)
    def linkTaskName(sourceSet: SourceSet): String = sourceSet.getTaskName("link", "")

    // TODO look into link tasks self-registering run/test counterparts - rules?

    def registerTask[T <: BackendTask](
      classOpt: Option[Class[? <: T]],
      name: String,
      before: String,
      after: String,
      group: String,
      dependsOn: Option[TaskProvider[?]] = None,
      replace: Boolean = false
    ): Option[TaskProvider[?]] = classOpt.map: (clazz: Class[? <: T]) =>
      project.getTasks.withType(clazz).configureEach((task: BackendTask) =>
        task.setDescription(s"$before ${backend.name} code$after.")
        task.setGroup(group)
      )

      val action: Action[T] = (task: T) => dependsOn.foreach(task.dependsOn(_))

      if !replace then
        project.getTasks.register(name, clazz, action)
      else
        project.getTasks.replace(name, clazz)
        project.getTasks.withType(clazz).named(name, action)

    // Register 'link' task.
    val link: Option[TaskProvider[?]] = registerTask(
      classOpt = delegate.linkTaskClassOpt,
      name = linkTaskName(mainSourceSet),
      before = "Links ",
      after = "",
      group = "build"
    )

    // Register 'run' task.
    registerTask(
      classOpt = delegate.runTaskClassOpt,
      name = "run",
      before = "Runs",
      after = "",
      group = "other",
      dependsOn = link
    )

    // Register 'testLink' task.
    val linkTest: Option[TaskProvider[?]] = registerTask(
      classOpt = delegate.testLinkTaskClassOpt,
      name = linkTaskName(testSourceSet),
      before = "Links test",
      after = "",
      group = "build"
    )

    // Replace 'test' task.
    // Test task and test source set are named the same.
    registerTask(
      classOpt = Some(delegate.testTaskClass),
      name = testSourceSet.getName,
      before = "Tests",
      after = " using sbt frameworks",
      group = LifecycleBasePlugin.VERIFICATION_GROUP,
      dependsOn = linkTest,
      replace = true
    )

  private def dependencyRequirements(
    projectScalaVersion: ScalaVersion,
    pluginScalaVersion: ScalaVersion,
    pluginDependenciesConfigurationName: Option[String]
  ): Seq[DependencyRequirements] =
    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = Sources.getSourceSets(project)
    val implementationConfigurationName    : String = mainSourceSet.getImplementationConfigurationName
    val testImplementationConfigurationName: String = testSourceSet.getImplementationConfigurationName
    val testRuntimeOnlyConfigurationName   : String = testSourceSet.getRuntimeOnlyConfigurationName

    val requirements: BackendDependencyRequirements = backend.dependencyRequirements(
      implementationConfiguration     = project.getConfigurations.getByName(implementationConfigurationName    ),
      testImplementationConfiguration = project.getConfigurations.getByName(testImplementationConfigurationName),
      scalaVersion = projectScalaVersion
    )

    pluginDependenciesConfigurationName.toSeq.map(
      DependencyRequirements(requirements.pluginDependencies      , pluginScalaVersion , _)
    ) ++ Seq(
      DependencyRequirements(requirements.implementation          , projectScalaVersion, implementationConfigurationName          ),
      DependencyRequirements(requirements.testRuntimeOnly         , projectScalaVersion, testRuntimeOnlyConfigurationName         ),
      DependencyRequirements(requirements.scalaCompilerPlugins    , projectScalaVersion, scalaCompilerPluginsConfigurationName    ),
      DependencyRequirements(requirements.testScalaCompilerPlugins, projectScalaVersion, testScalaCompilerPluginsConfigurationName)
    )
  
  private def configureScalaCompile(scalaVersion: ScalaVersion): Unit =
    val scalaCompileParameters: Seq[String] = backend.scalaCompileParameters(scalaVersion)
    def ensureParameters(scalaCompile: ScalaCompile): Unit =
      this.ensureParameters(scalaCompile, scalaCompileParameters, project.getLogger)

    def addScalaCompilerPlugins(scalaCompile: ScalaCompile, configurationName: String): Unit =
      this.addScalaCompilerPlugins(project.getConfigurations.getByName(configurationName), scalaCompile, project.getLogger)

    def scalaCompile(sourceSet: SourceSet): ScalaCompile =
      project.getTasks.withType(classOf[ScalaCompile]).findByName(sourceSet.getCompileTaskName("scala"))

    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = Sources.getSourceSets(project)

    val mainScalaCompile: ScalaCompile = scalaCompile(mainSourceSet)
    ensureParameters(mainScalaCompile)
    addScalaCompilerPlugins(mainScalaCompile, scalaCompilerPluginsConfigurationName)

    val testScalaCompile: ScalaCompile = scalaCompile(testSourceSet)
    ensureParameters(testScalaCompile)
    addScalaCompilerPlugins(testScalaCompile, scalaCompilerPluginsConfigurationName)
    addScalaCompilerPlugins(testScalaCompile, testScalaCompilerPluginsConfigurationName)

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
    // I am not sure that even this is needed for the pre-existing `scalaCompilerPlugins` configuration.
    val scalaCompilerPlugins: Iterable[File] = scalaCompilerPluginsConfiguration.asScala
    if scalaCompilerPlugins.nonEmpty then
      logger.info(s"Adding ${scalaCompilerPluginsConfiguration.getName} to ${scalaCompile.getName}: $scalaCompilerPlugins.")
      val plugins: FileCollection = Option(scalaCompile.getScalaCompilerPlugins)
        .map((existingPlugins: FileCollection) => existingPlugins.plus(scalaCompilerPluginsConfiguration))
        .getOrElse(scalaCompilerPluginsConfiguration)
      scalaCompile.setScalaCompilerPlugins(plugins)
