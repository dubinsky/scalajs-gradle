package org.podval.tools.nonjvm

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.tasks.TaskProvider
import org.podval.tools.build.{Backend, DependencyRequirement, JarTask, ScalaBinaryVersion, ScalaDependency, ScalaLibrary, Version}
import org.podval.tools.util.{Classpath, Configurations, Tasks}
import org.podval.tools.util.Scala212Collections.{arrayConcat, arrayMap}
import scala.jdk.CollectionConverters.IterableHasAsScala

abstract class NonJvmBackend(
  name: String,
  group: String,
  final val versionDefault: Version,
  sourceRoot: String,
  artifactSuffix: String,
  pluginDependenciesConfigurationName: String,
  areCompilerPluginsBuiltIntoScala3: Boolean
) extends Backend(
  name = name,
  sourceRoot = sourceRoot,
  artifactSuffix = Some(artifactSuffix),
  testsCanNotBeForked = true,
  expandClasspathForTestEnvironment = false
):
  protected def linkTaskClass         : Class[? <: LinkTask.Main[this.type]]
  protected def testLinkTaskClass     : Class[? <: LinkTask.Test[this.type]]
  protected def runTaskClass          : Class[? <: RunTask .Main[this.type, ? <: LinkTask.Main[this.type]]]
  override protected def testTaskClass: Class[? <: RunTask .Test[this.type, ? <: LinkTask.Test[this.type]]]

  protected def compilerPlugin: ScalaDependency
  protected def junit4Plugin: ScalaDependency
  protected def linker: ScalaDependency
  protected def testAdapter: ScalaDependency
  protected def testBridge: ScalaDependency

  protected def library(scalaLibrary: ScalaLibrary): ScalaDependency

  protected def pluginDependencies: Array[ScalaDependency]
  protected def withBackendVersion: Array[ScalaDependency]
  protected def withDefaultVersion: Array[ScalaDependency]

  final def scalaDependency(
    what: String,
    group: String = this.group,
    artifact: String,
    versionDefault: Version = this.versionDefault
  ): ScalaDependency = ScalaDependency(
    backend = this,
    name = s"$name $what",
    group = group,
    versionDefault = versionDefault,
    artifact = artifact
  )

  protected def junit4: NonJvmJUnit4TestFramework

  protected def implementation(scalaLibrary: ScalaLibrary): Array[DependencyRequirement]

  protected def scalaCompileParameters(scalaLibrary: ScalaLibrary): Seq[String]

  final def backendVersion(
    project: Project,
    scalaLibrary: ScalaLibrary
  ): Version =
    val libraryDependency: ScalaDependency = library(scalaLibrary)
    libraryDependency
      .findInConfiguration(Configurations.implementation(project))
      .map(_.version.version)
      .getOrElse(libraryDependency.versionDefault)

  final def junit4present(project: Project): Boolean = junit4
    .dependency
    .findInConfiguration(Configurations.testImplementation(project))
    .isDefined

  override def apply(
    project: Project, 
    jvmPluginServices: JvmPluginServices,
    isRunningInIntelliJ: Boolean
  ): Unit =
    super.apply(project, jvmPluginServices, isRunningInIntelliJ)

    LinkTask.configureTasks(project)

    JarTask.configureTasks(project, this)

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

  final override def afterEvaluate(
    project: Project, 
    projectScalaLibrary: ScalaLibrary, 
    pluginScalaLibrary: ScalaLibrary
  ): Unit =
    super.afterEvaluate(project, projectScalaLibrary, pluginScalaLibrary)

    ScalaCompiles.configure(project, scalaCompileParameters(projectScalaLibrary))

    Classpath.addTo(Configurations.configuration(project, pluginDependenciesConfigurationName).asScala)
    projectScalaLibrary.verify(project)

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

  final override protected def requirements(
    project: Project,
    projectScalaLibrary: ScalaLibrary,
    pluginScalaLibrary: ScalaLibrary
  ): Seq[DependencyRequirement.Many] =
    val isProjectScala3: Boolean = projectScalaLibrary.scalaVersion.binaryVersion match
      case _: ScalaBinaryVersion.Scala3 => true
      case _ => false

    val backendVersion: Version = NonJvmBackend.this.backendVersion(project, projectScalaLibrary)
    
    def one(configurationName: String, dependency: ScalaDependency) = DependencyRequirement.Many(
      configurationName = configurationName,
      scalaLibrary = projectScalaLibrary,
      requirements = Array(dependency.require(backendVersion))
    )

    Seq(
      DependencyRequirement.Many(
        configurationName = pluginDependenciesConfigurationName,
        scalaLibrary = pluginScalaLibrary,
        requirements = arrayConcat(
          arrayMap(Array(linker, testAdapter),
            _.jvm.require(backendVersion)),
          arrayMap(pluginDependencies,
            _.jvm.require())
        )
      ),
      DependencyRequirement.Many(
        configurationName = Configurations.implementationName(project),
        scalaLibrary = projectScalaLibrary,
        requirements = arrayConcat(
          arrayConcat(
            arrayMap(arrayConcat(Array(library(projectScalaLibrary)), withBackendVersion),
              _.require(backendVersion)),
            arrayMap(withDefaultVersion,
              _.require())
          ),
          implementation(projectScalaLibrary)
        )
      ),
      one(Configurations.testRuntimeOnlyName(project), testBridge)
    ) ++
    (if areCompilerPluginsBuiltIntoScala3 && isProjectScala3 then Seq.empty else
      Seq(one(Configurations.scalaCompilerPluginsName, compilerPlugin.scalaCompilerPlugin)) ++
      (if !junit4present(project) then Seq.empty else
        Seq(one(Configurations.testScalaCompilerPluginsName, junit4Plugin.scalaCompilerPlugin))
      )
    )
