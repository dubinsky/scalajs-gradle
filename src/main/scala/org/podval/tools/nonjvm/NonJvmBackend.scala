package org.podval.tools.nonjvm

import org.gradle.api.Project
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.podval.tools.build.{ClasspathAddition, DependencyRequirement, PreVersion, ScalaBackend, ScalaDependencyMaker,
  ScalaLibrary, ScalaVersion, Version}
import org.podval.tools.gradle.{Archive, Configurations, ScalaCompiles, TaskWithSourceSet, Tasks}
import org.podval.tools.test.framework.FrameworkDescriptor
import org.podval.tools.util.Scala212Collections.{arrayConcat, arrayMap}

abstract class NonJvmBackend(
  name: String,
  sourceRoot: String,
  artifactSuffix: String,
  pluginDependenciesConfigurationName: String,
  areCompilerPluginsBuiltIntoScala3: Boolean,
  junit4: FrameworkDescriptor,
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
  expandClasspathForTestEnvironment = false
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
    project: Project,
    scalaVersion: ScalaVersion
  ): Version =
    val libraryDependency: ScalaDependencyMaker = library(scalaVersion)
    libraryDependency
      .findable
      .findInConfiguration(Configurations.implementation(project))
      .map(_.version)
      .map(versionExtractor)
      .getOrElse(libraryDependency.versionDefaultFor(scalaVersion))

  final def junit4present(project: Project): Boolean = junit4
    .maker(this)
    .get
    .findable
    .findInConfiguration(Configurations.testImplementation(project)).isDefined

  override def apply(project: Project, jvmPluginServices: JvmPluginServices): Unit =
    super.apply(project, jvmPluginServices)

    // Configure JAR appendix.
    Tasks.configureEach(
      project, 
      classOf[Jar],
      Tasks.conventionProvider(
        _,
        _.getArchiveAppendix,
        jar => if jar.getArchiveClassifier.isPresent then sourceRoot else null,
        project
      )
    )

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

  override def afterEvaluate(project: Project, scalaLibrary: ScalaLibrary): Unit =
    super.afterEvaluate(project, scalaLibrary)

    ScalaCompiles.configure(project, scalaCompileParameters(scalaLibrary.scalaVersion))

    ClasspathAddition.Many(Seq(
      ClasspathAddition(pluginDependenciesConfigurationName)
    )).apply(
      project,
      scalaLibrary
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
    
    def one(configurationName: String, dependency: ScalaDependencyMaker) = DependencyRequirement.Many(
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
      Seq(one(Configurations.scalaCompilerPluginsName, compiler)) ++
      (if !junit4present(project) then Seq.empty else
        Seq(one(Configurations.testScalaCompilerPluginsName, junit4Plugin))
      )
    )
