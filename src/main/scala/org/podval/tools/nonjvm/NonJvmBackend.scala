package org.podval.tools.nonjvm

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.tasks.TaskProvider
import org.podval.tools.build.{DependencyRequirement, ScalaBackend, ScalaDependency, ScalaLibrary, Version}
import org.podval.tools.gradle.{Archive, Configurations, GradleClasspath, ScalaCompiles, Tasks}
import org.podval.tools.test.framework.NonJvmJUnit4Framework
import org.podval.tools.platform.Scala212Collections.{arrayConcat, arrayMap}
import org.podval.tools.task.TaskWithSourceSet
import NonJvmBackend.Dep

abstract class NonJvmBackend(
  name: String,
  val group: String,
  val versionDefault: Version,
  sourceRoot: String,
  artifactSuffix: String,
  pluginDependenciesConfigurationName: String,
  areCompilerPluginsBuiltIntoScala3: Boolean,
  libraryScala3 : Dep,
  libraryScala2 : Dep,
  compilerPlugin: Dep,
  junit4Plugin  : Dep,
  linker        : Dep,
  testAdapter   : Dep,
  testBridge    : Dep,
  pluginDependencies: Array[Dep],
  withBackendVersion: Array[Dep],
  withDefaultVersion: Array[Dep]
) extends ScalaBackend(
  name = name,
  sourceRoot = sourceRoot,
  artifactSuffix = Some(artifactSuffix),
  testsCanNotBeForked = true,
  expandClasspathForTestEnvironment = false
):
  final override def isJvm: Boolean = false

  protected def junit4: NonJvmJUnit4Framework

  protected def linkTaskClass         : Class[? <: LinkTask.Main[this.type]]
  protected def testLinkTaskClass     : Class[? <: LinkTask.Test[this.type]]
  protected def runTaskClass          : Class[? <: RunTask .Main[this.type, ? <: LinkTask.Main[this.type]]]
  override protected def testTaskClass: Class[? <: RunTask .Test[this.type, ? <: LinkTask.Test[this.type]]]

  protected def implementation(scalaLibrary: ScalaLibrary): Array[DependencyRequirement]

  protected def scalaCompileParameters(scalaLibrary: ScalaLibrary): Seq[String]

  private def library(scalaLibrary: ScalaLibrary): Dep =
    if scalaLibrary.isScala3 then libraryScala3 else libraryScala2

  final def backendVersion(
    project: Project,
    scalaLibrary: ScalaLibrary
  ): Version =
    val libraryDependency: ScalaDependency = library(scalaLibrary)(this)
    libraryDependency
      .findInConfiguration(Configurations.implementation(project))
      .map(_.version.version)
      .getOrElse(libraryDependency.versionDefaultFor(this, scalaLibrary))

  final def junit4present(project: Project): Boolean = junit4
    .findInConfiguration(Configurations.testImplementation(project))
    .isDefined

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

  final override def afterEvaluate(
    project: Project, 
    projectScalaLibrary: ScalaLibrary, 
    pluginScalaLibrary: ScalaLibrary
  ): Unit =
    super.afterEvaluate(project, projectScalaLibrary, pluginScalaLibrary)
    ScalaCompiles.configure(project, scalaCompileParameters(projectScalaLibrary))
    GradleClasspath.addTo(project, pluginDependenciesConfigurationName)
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

  final override protected def dependencyRequirements(
    project: Project,
    projectScalaLibrary: ScalaLibrary,
    pluginScalaLibrary: ScalaLibrary
  ): Seq[DependencyRequirement.Many] =
    val backendVersion: Version = NonJvmBackend.this.backendVersion(project, projectScalaLibrary)
    
    def one(configurationName: String, dependency: ScalaDependency) = DependencyRequirement.Many(
      configurationName = configurationName,
      scalaLibrary = projectScalaLibrary,
      dependencyRequirements = Array(dependency.required(backendVersion))
    )

    Seq(
      DependencyRequirement.Many(
        configurationName = pluginDependenciesConfigurationName,
        scalaLibrary = pluginScalaLibrary,
        dependencyRequirements = arrayConcat(
          arrayMap(Array(linker, testAdapter),
            _(this).jvm.required(backendVersion)),
          arrayMap(pluginDependencies,
            _(this).jvm.required())
        )
      ),
      DependencyRequirement.Many(
        configurationName = Configurations.implementationName(project),
        scalaLibrary = projectScalaLibrary,
        dependencyRequirements = arrayConcat(
          arrayConcat(
            arrayMap(arrayConcat(Array(library(projectScalaLibrary)), withBackendVersion),
              _(this).required(backendVersion)),
            arrayMap(withDefaultVersion,
              _(this).required())
          ),
          implementation(projectScalaLibrary)
        )
      ),
      one(Configurations.testRuntimeOnlyName(project), testBridge(this))
    ) ++
    (if areCompilerPluginsBuiltIntoScala3 && projectScalaLibrary.isScala3 then Seq.empty else
      Seq(one(Configurations.scalaCompilerPluginsName, compilerPlugin(this).scalaCompilerPlugin)) ++
      (if !junit4present(project) then Seq.empty else
        Seq(one(Configurations.testScalaCompilerPluginsName, junit4Plugin(this).scalaCompilerPlugin))
      )
    )

object NonJvmBackend:
  class Dep(
    artifact: String,
    what: String,
    transformer: ScalaDependency => ScalaDependency = identity,
    groupOverride: Option[String] = None,
    versionOverride: Option[Version] = None
  ):
    final def versionDefault: Version = versionOverride.get

    def apply(nonJvmBackend: NonJvmBackend): ScalaDependency = transformer(ScalaDependency(
      backend = nonJvmBackend,
      artifactId = artifact,
      groupId = groupOverride.getOrElse(nonJvmBackend.group),
      version = versionOverride.getOrElse(nonJvmBackend.versionDefault),
      what = what
    ))
