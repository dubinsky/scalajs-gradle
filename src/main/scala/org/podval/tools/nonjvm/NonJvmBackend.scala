package org.podval.tools.nonjvm

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.tasks.TaskProvider
import org.podval.tools.build.{ClasspathAddition, DependencyMaker, DependencyRequirement, PreVersion, ScalaBackend,
  ScalaDependencyMaker, ScalaLibrary, ScalaVersion, Version}
import org.podval.tools.gradle.{Archive, Configurations, ScalaCompiles, TaskWithSourceSet, Tasks}
import org.podval.tools.test.framework.NonJvmJUnit4FrameworkDescriptor
import org.podval.tools.util.Scala212Collections.{arrayConcat, arrayMap}

abstract class NonJvmBackend(
  name: String,
  val group: String,
  val versionDefault: Version,
  sourceRoot: String,
  artifactSuffix: String,
  pluginDependenciesConfigurationName: String,
  areCompilerPluginsBuiltIntoScala3: Boolean
) extends ScalaBackend(
  name = name,
  sourceRoot = sourceRoot,
  artifactSuffix = Some(artifactSuffix),
  testsCanNotBeForked = true,
  expandClasspathForTestEnvironment = false
):
  class BackendDependency(
    final override val artifact: String,
    what: String
  ) extends ScalaDependencyMaker:
    override def scalaBackend: ScalaBackend = NonJvmBackend.this
    override def versionDefault: Version    = NonJvmBackend.this.versionDefault
    final override def description: String  = NonJvmBackend.this.describe(what)
    final override def group: String        = NonJvmBackend.this.group

  class Jvm(artifact: String, what: String) extends BackendDependency(artifact, what) with DependencyMaker.Jvm

  trait Plugin extends Jvm with ScalaDependencyMaker.IsScalaVersionFull

  protected def linkTaskClass         : Class[? <: LinkTask.Main[this.type]]
  protected def testLinkTaskClass     : Class[? <: LinkTask.Test[this.type]]
  protected def runTaskClass          : Class[? <: RunTask .Main[this.type, ? <: LinkTask.Main[this.type]]]
  override protected def testTaskClass: Class[? <: RunTask .Test[this.type, ? <: LinkTask.Test[this.type]]]

  protected def versionExtractor(version: PreVersion): Version
  protected def versionComposer(projectScalaVersion: ScalaVersion, backendVersion: Version): PreVersion

  protected def junit4: NonJvmJUnit4FrameworkDescriptor
  protected def library(isScala3: Boolean): BackendDependency
  protected def compilerPlugin: Plugin
  protected def junit4Plugin: Plugin
  protected def linker: Jvm
  protected def testAdapter: Jvm
  protected def testBridge: BackendDependency
  protected def pluginDependencies: Array[Jvm]
  protected def implementation: Array[BackendDependency]
  protected def implementationDependencyRequirements(scalaVersion: ScalaVersion): Array[DependencyRequirement]

  protected def scalaCompileParameters(scalaVersion: ScalaVersion): Seq[String]

  private def library(scalaVersion: ScalaVersion): BackendDependency = library(scalaVersion.isScala3)

  final def backendVersion(
    project: Project,
    scalaVersion: ScalaVersion
  ): Version =
    val libraryDependency: BackendDependency = library(scalaVersion)
    libraryDependency
      .findable
      .findInConfiguration(Configurations.implementation(project))
      .map(_.version)
      .map(versionExtractor)
      .getOrElse(libraryDependency.versionDefaultFor(this, scalaVersion))

  final def junit4present(project: Project): Boolean = junit4
    .maker(this)
    .get
    .findable
    .findInConfiguration(Configurations.testImplementation(project)).isDefined

  override def apply(
    project: Project, 
    jvmPluginServices: JvmPluginServices,
    isRunningInIntelliJ: Boolean
  ): Unit =
    super.apply(project, jvmPluginServices, isRunningInIntelliJ)

    Archive.configureAppendix(project, sourceRoot)
    TaskWithSourceSet.configureTasks(project)

    // Create Plugin Dependencies Configuration.
    Configurations.create(
      project = project,
      configurationName = pluginDependenciesConfigurationName,
      isTransitive = true,
      description = s"$name dependencies.",
      jvmPluginServices = None
    )

    // Create Test Scala Compiler Plugins Configuration.
    Configurations.create(
      project = project,
      configurationName = Configurations.testScalaCompilerPluginsName,
      isTransitive = false,
      description = "Test Scala Compiler Plugins",
      jvmPluginServices = Some(jvmPluginServices)
    )

  override def afterEvaluate(
    project: Project, 
    projectScalaLibrary: ScalaLibrary, 
    pluginScalaLibrary: ScalaLibrary
  ): Unit =
    super.afterEvaluate(project, projectScalaLibrary, pluginScalaLibrary)

    ScalaCompiles.configure(project, scalaCompileParameters(projectScalaLibrary.scalaVersion))

    ClasspathAddition.Many(Seq(
      ClasspathAddition(pluginDependenciesConfigurationName)
    )).apply(
      project,
      projectScalaLibrary
    )

  // TODO look into link tasks self-registering run/test counterparts - rules?
  final override def registerTasks(project: Project): Unit =
    def linkTaskName(isTest: Boolean): String = Tasks.taskName(project, "link", isTest)

    // Register 'link' task.
    val link: TaskProvider[?] = registerTask(
      project,
      taskClass = linkTaskClass,
      taskName = linkTaskName(isTest = false),
      before = "Links",
      after = "",
      group = Tasks.buildGroup
    )

    // Register 'run' task.
    registerTask(
      project,
      taskClass = runTaskClass,
      taskName = "run",
      before = "Runs",
      after = "",
      group = Tasks.otherGroup,
      dependsOn = Some(link)
    )

    // Register 'testLink' task.
    val linkTest: TaskProvider[?] = registerTask(
      project,
      taskClass = testLinkTaskClass,
      taskName = linkTaskName(isTest = true),
      before = "Links test",
      after = "",
      group = Tasks.buildGroup
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
    val backendVersion: Version = NonJvmBackend.this.backendVersion(project, projectScalaVersion)
    
    def one(configurationName: String, dependency: BackendDependency) = DependencyRequirement.Many(
      configurationName = configurationName,
      scalaVersion = projectScalaVersion,
      dependencyRequirements = Array(dependency.required(backendVersion))
    )

    Seq(
      DependencyRequirement.Many(
        configurationName = pluginDependenciesConfigurationName,
        scalaVersion = pluginScalaVersion,
        dependencyRequirements = arrayConcat(
          arrayMap(Array(linker, testAdapter), _.required(backendVersion)),
          arrayMap(pluginDependencies, _.required())
        )
      ),
      DependencyRequirement.Many(
        configurationName = Configurations.implementationName(project),
        scalaVersion = projectScalaVersion,
        dependencyRequirements = arrayConcat(
          arrayConcat(
            Array(library(projectScalaVersion).required(versionComposer(projectScalaVersion, backendVersion))),
            arrayMap(implementation, _.required(backendVersion))
          ),
          implementationDependencyRequirements(projectScalaVersion)
        )
      ),
      one(Configurations.testRuntimeOnlyName(project), testBridge)
    ) ++
    (if areCompilerPluginsBuiltIntoScala3 && projectScalaVersion.isScala3 then Seq.empty else
      Seq(one(Configurations.scalaCompilerPluginsName, compilerPlugin)) ++
      (if !junit4present(project) then Seq.empty else
        Seq(one(Configurations.testScalaCompilerPluginsName, junit4Plugin))
      )
    )
