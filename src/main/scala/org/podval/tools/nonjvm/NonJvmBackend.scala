package org.podval.tools.nonjvm

import org.gradle.api.{Action, Project}
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.{SourceSet, TaskProvider}
import org.gradle.jvm.tasks.Jar
import org.gradle.util.internal.GUtil
import org.podval.tools.build.{ClassPathAddition, DependencyRequirement, PreVersion, ScalaBackend, ScalaDependencyMaker,
  ScalaLibrary, ScalaVersion, SourceSets, Version}
import org.podval.tools.test.framework.FrameworkDescriptor
import org.podval.tools.util.Scala212Collections.{arrayConcat, arrayMap}
import org.slf4j.Logger
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala, SeqHasAsJava}
import java.io.File

abstract class NonJvmBackend(
  name: String,
  sourceRoot: String,
  artifactSuffix: String,
  pluginDependenciesConfigurationName: String,
  areCompilerPluginsBuiltIntoScala3: Boolean,
  junit4: FrameworkDescriptor,
  val versionDefault: Version,
  libraryScala3: ScalaDependencyMaker,
  libraryScala2: ScalaDependencyMaker,
  compiler     : ScalaDependencyMaker,
  linker       : ScalaDependencyMaker,
  testAdapter  : ScalaDependencyMaker,
  testBridge   : ScalaDependencyMaker,
  junit4Plugin : ScalaDependencyMaker,
  pluginDependencies: Array[ScalaDependencyMaker],
  implementation: Array[ScalaDependencyMaker]
) extends ScalaBackend(
  name = name,
  sourceRoot = sourceRoot,
  artifactSuffix = Some(artifactSuffix),
  testsCanNotBeForked = true,
  expandClassPathForTestEnvironment = false
):
  protected def linkTaskClass         : Class[? <: LinkTask.Main[this.type]]
  protected def testLinkTaskClass     : Class[? <: LinkTask.Test[this.type]]
  protected def runTaskClass          : Class[? <: RunTask .Main[this.type, ? <: LinkTask.Main[this.type]]]
  override protected def testTaskClass: Class[? <: RunTask .Test[this.type, ? <: LinkTask.Test[this.type]]]

  protected def versionExtractor(version: PreVersion): Version
  protected def versionComposer(projectScalaVersion: ScalaVersion, backendVersion: Version): PreVersion

  protected def implementationDependencyRequirements(scalaVersion: ScalaVersion): Array[DependencyRequirement]

  protected def scalaCompileParameters(scalaVersion: ScalaVersion): Seq[String]

  private def library(scalaVersion: ScalaVersion): ScalaDependencyMaker =
    if scalaVersion.isScala3
    then libraryScala3
    else libraryScala2

  final def backendVersion(
    scalaVersion: ScalaVersion,
    implementationConfiguration: Configuration
  ): Version =
    val libraryDependency: ScalaDependencyMaker = library(scalaVersion)
    libraryDependency
      .findable
      .findInConfiguration(implementationConfiguration)
      .map(_.version)
      .map(versionExtractor)
      .getOrElse(libraryDependency.versionDefault)

  final def junit4present(
    testImplementationConfiguration: Configuration
  ): Boolean = junit4
    .maker(this)
    .get
    .findable
    .findInConfiguration(testImplementationConfiguration).isDefined

  override def apply(project: Project, jvmPluginServices: JvmPluginServices): Unit =
    super.apply(project, jvmPluginServices)

    configureArchiveAppendix(project)
    configureLinkTasks(project)
    createPluginDependenciesConfiguration(project)
    createTestScalaCompilerPluginsConfiguration(project, jvmPluginServices)

  private def configureArchiveAppendix(project: Project): Unit =
    val archiveAppendixConvention: Action[Jar] = (jar: Jar) => jar
      .getArchiveAppendix
      .convention(project.provider(() => if jar.getArchiveClassifier.isPresent then sourceRoot else null))

    project
      .getTasks
      .withType(classOf[Jar])
      .configureEach(archiveAppendixConvention)

  private def configureLinkTasks(project: Project): Unit = project
    .getTasks
    .withType(classOf[LinkTask[this.type]])
    .configureEach((task: LinkTask[this.type]) =>
      val sourceSet: SourceSet = SourceSets.get(project, task.isTest)
      task.dependsOn(project.getTasks.named(sourceSet.getClassesTaskName))
      task.getRuntimeClassPath.setFrom(sourceSet.getRuntimeClasspath)
    )

  private def createPluginDependenciesConfiguration(project: Project): Unit =
    val configuration: Configuration = project.getConfigurations.create(pluginDependenciesConfigurationName)
    configuration.setTransitive(true)
    configuration.setCanBeResolved(true) // TODO should be false
    configuration.setCanBeDeclared(true)
    configuration.setCanBeConsumed(false)
    configuration.setDescription(s"$name dependencies.")

  private def scalaCompilerPluginsConfigurationName: String = ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME
  private def testScalaCompilerPluginsConfigurationName: String = GUtil.toLowerCamelCase(s"test $scalaCompilerPluginsConfigurationName")

  private def createTestScalaCompilerPluginsConfiguration(project: Project, jvmPluginServices: JvmPluginServices): Unit =
    val configuration: Configuration = project.getConfigurations.create(testScalaCompilerPluginsConfigurationName)
    configuration.setTransitive(false)
    configuration.setCanBeResolved(true) // TODO should be false; when (and if) ScalaBasePlugin is cleaned up, copy it here.
    configuration.setCanBeDeclared(true)
    configuration.setCanBeConsumed(false)
    jvmPluginServices.configureAsRuntimeClasspath(configuration)

  override def afterEvaluate(project: Project, scalaLibrary: ScalaLibrary): Unit =
    super.afterEvaluate(project, scalaLibrary)

    configureScalaCompile(project, scalaLibrary.scalaVersion)

    ClassPathAddition.Many(Seq(ClassPathAddition(
     configurationName = pluginDependenciesConfigurationName,
     classPathConfigurationName = SourceSets.runtimeClasspathConfigurationName(project)
    )))
      .apply(
        project,
        scalaLibrary
      )

  private def configureScalaCompile(project: Project, scalaVersion: ScalaVersion): Unit =
    val scalaCompileParameters: Seq[String] = this.scalaCompileParameters(scalaVersion)

    def ensureParameters(scalaCompile: ScalaCompile): Unit =
      this.ensureParameters(scalaCompile, scalaCompileParameters, project.getLogger)

    def addScalaCompilerPlugins(scalaCompile: ScalaCompile, configurationName: String): Unit =
      this.addScalaCompilerPlugins(SourceSets.getConfiguration(project, configurationName), scalaCompile, project.getLogger)

    def scalaCompile(sourceSet: SourceSet): ScalaCompile =
      project.getTasks.withType(classOf[ScalaCompile]).findByName(sourceSet.getCompileTaskName("scala"))

    val mainScalaCompile: ScalaCompile = scalaCompile(SourceSets.mainSourceSet(project))
    ensureParameters(mainScalaCompile)
    addScalaCompilerPlugins(mainScalaCompile, scalaCompilerPluginsConfigurationName)

    val testScalaCompile: ScalaCompile = scalaCompile(SourceSets.testSourceSet(project))
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
      scalaCompile.setScalaCompilerPlugins(Option(scalaCompile.getScalaCompilerPlugins)
        .map(_.plus(scalaCompilerPluginsConfiguration))
        .getOrElse(scalaCompilerPluginsConfiguration)
      )

  // TODO look into link tasks self-registering run/test counterparts - rules?
  final override def registerTasks(project: Project): Unit =
    def linkTaskName(isTest: Boolean): String = SourceSets.get(project, isTest).getTaskName("link", "")

    // Register 'link' task.
    val link: TaskProvider[?] = registerTask(
      project,
      taskClass = linkTaskClass,
      taskName = linkTaskName(isTest = false),
      before = "Links",
      after = "",
      group = "build"
    )

    // Register 'run' task.
    registerTask(
      project,
      taskClass = runTaskClass,
      taskName = "run",
      before = "Runs",
      after = "",
      group = "other",
      dependsOn = Some(link)
    )

    // Register 'testLink' task.
    val linkTest: TaskProvider[?] = registerTask(
      project,
      taskClass = testLinkTaskClass,
      taskName = linkTaskName(isTest = true),
      before = "Links test",
      after = "",
      group = "build"
    )

    // Replace 'test' task.
    registerTestTask(
      project,
      dependsOn = Some(linkTest)
    )

  // Add JUnit4 compiler plugin:
  // only when JUnit4 is in use, otherwise with Scala.js `testClasses` task throws
  //   "scala.reflect.internal.MissingRequirementError: object org.junit.Test in compiler mirror not found.";
  // only to a separate `testScalaCompilerPlugins` configuration to avoid the error when compiling main sources.
  final override protected def dependencyRequirements(
    project: Project,
    projectScalaVersion: ScalaVersion,
    pluginScalaVersion: ScalaVersion
  ): Seq[DependencyRequirement.Many] =
    val implementationConfigurationName: String = SourceSets.implementationConfigurationName(project)
    val implementationConfiguration: Configuration = SourceSets.getConfiguration(project, implementationConfigurationName)
    val testImplementationConfigurationName: String = SourceSets.testImplementationConfigurationName(project)
    val testImplementationConfiguration: Configuration = SourceSets.getConfiguration(project, testImplementationConfigurationName)
    val testRuntimeOnlyConfigurationName: String = SourceSets.testRuntimeOnlyConfigurationName(project)

    val backendVersion: Version = NonJvmBackend.this.backendVersion(
      projectScalaVersion,
      implementationConfiguration
    )

    def withBackendVersion(what: Array[ScalaDependencyMaker]) = arrayMap(what, _.required(backendVersion))

    val addScalaCompilerPlugins: Boolean = !areCompilerPluginsBuiltIntoScala3 || !projectScalaVersion.isScala3

    Seq(
      DependencyRequirement.Many(
        configurationName = pluginDependenciesConfigurationName,
        scalaVersion = pluginScalaVersion,
        dependencyRequirements = arrayConcat(
          withBackendVersion(Array(linker, testAdapter)),
          arrayMap(pluginDependencies, _.required())
        )
      ),
      DependencyRequirement.Many(
        configurationName = implementationConfigurationName,
        scalaVersion = projectScalaVersion,
        dependencyRequirements = arrayConcat(
          arrayConcat(
            Array(library(projectScalaVersion).required(versionComposer(projectScalaVersion, backendVersion))),
            withBackendVersion(implementation)
          ),
          implementationDependencyRequirements(projectScalaVersion)
        )
      ),
      DependencyRequirement.Many(
        configurationName = testRuntimeOnlyConfigurationName,
        scalaVersion = projectScalaVersion,
        dependencyRequirements = withBackendVersion(
          Array(testBridge)
        )
      ),
      DependencyRequirement.Many(
        configurationName = scalaCompilerPluginsConfigurationName,
        scalaVersion = projectScalaVersion,
        dependencyRequirements = withBackendVersion(
          if !addScalaCompilerPlugins
          then Array.empty
          else Array(compiler)
        )
      ),
      DependencyRequirement.Many(
        configurationName = testScalaCompilerPluginsConfigurationName,
        scalaVersion = projectScalaVersion,
        dependencyRequirements = withBackendVersion(
          if !addScalaCompilerPlugins || !junit4present(testImplementationConfiguration)
          then Array.empty
          else Array(junit4Plugin)
        )
      )
    )
