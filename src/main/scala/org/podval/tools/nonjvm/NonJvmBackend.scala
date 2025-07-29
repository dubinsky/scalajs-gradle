package org.podval.tools.nonjvm

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.tasks.TaskProvider
import org.podval.tools.build.{DependencyRequirement, PreVersion, ScalaBackend, ScalaDependencyMaker, ScalaLibrary, Version}
import org.podval.tools.gradle.{Archive, Configurations, GradleClasspath, ScalaCompiles, TaskWithSourceSet, Tasks}
import org.podval.tools.test.framework.NonJvmJUnit4FrameworkDescriptor
import org.podval.tools.util.Scala212Collections.{arrayConcat, arrayMap}
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
  protected def linkTaskClass         : Class[? <: LinkTask.Main[this.type]]
  protected def testLinkTaskClass     : Class[? <: LinkTask.Test[this.type]]
  protected def runTaskClass          : Class[? <: RunTask .Main[this.type, ? <: LinkTask.Main[this.type]]]
  override protected def testTaskClass: Class[? <: RunTask .Test[this.type, ? <: LinkTask.Test[this.type]]]

  protected def versionExtractor(version: PreVersion): Version
  protected def versionComposer(scalaLibrary: ScalaLibrary, backendVersion: Version): PreVersion

  protected def junit4: NonJvmJUnit4FrameworkDescriptor

  protected def implementation(scalaLibrary: ScalaLibrary): Array[DependencyRequirement]

  protected def scalaCompileParameters(scalaLibrary: ScalaLibrary): Seq[String]

  private def library(scalaLibrary: ScalaLibrary): ScalaDependencyMaker =
    (if scalaLibrary.isScala3 then libraryScala3 else libraryScala2)(this)

  final def backendVersion(
    project: Project,
    scalaLibrary: ScalaLibrary
  ): Version =
    val libraryDependency: ScalaDependencyMaker = library(scalaLibrary)
    libraryDependency
      .findable
      .findInConfiguration(Configurations.implementation(project))
      .map(_.version)
      .map(versionExtractor)
      .getOrElse(libraryDependency.versionDefaultFor(this, scalaLibrary))

  final def junit4present(project: Project): Boolean = junit4
    .forBackend(this)
    .get
    .findable
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

  override def afterEvaluate(
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
    
    def one(configurationName: String, dependency: ScalaDependencyMaker) = DependencyRequirement.Many(
      configurationName = configurationName,
      scalaLibrary = projectScalaLibrary,
      dependencyRequirements = Array(dependency.required(backendVersion))
    )

    Seq(
      DependencyRequirement.Many(
        configurationName = pluginDependenciesConfigurationName,
        scalaLibrary = pluginScalaLibrary,
        dependencyRequirements = arrayConcat(
          arrayMap(Array(linker, testAdapter), _(this).jvm.required(backendVersion)),
          arrayMap(pluginDependencies, _(this).jvm.required())
        )
      ),
      DependencyRequirement.Many(
        configurationName = Configurations.implementationName(project),
        scalaLibrary = projectScalaLibrary,
        dependencyRequirements = arrayConcat(
          arrayConcat(
            arrayConcat(
              Array(library(projectScalaLibrary).required(versionComposer(projectScalaLibrary, backendVersion))),
              arrayMap(withBackendVersion, _(this).required(backendVersion))
            ),
            arrayMap(withDefaultVersion, _(this).required())
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
  final class Dep(
    val artifactId: String,
    val what: String,
    val transformer: ScalaDependencyMaker => ScalaDependencyMaker = identity
  ):
    def apply(backend: NonJvmBackend): ScalaDependencyMaker = transformer(
      new ScalaDependencyMaker:
        override def artifact: String = artifactId
        override def group: String = backend.group
        override def description: String = backend.describe(what)
        override def scalaBackend: ScalaBackend = backend
        override def versionDefault: Version = backend.versionDefault
    )
