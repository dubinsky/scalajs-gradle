package org.podval.tools.scalajsplugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.tasks.JvmConstants
import org.gradle.api.file.{DirectoryProperty, FileCollection}
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.provider.ProviderInternal
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.plugins.{ExtraPropertiesExtension, JavaBasePlugin, JavaPluginExtension, JvmTestSuitePlugin}
import org.gradle.api.plugins.scala.{ScalaBasePlugin, ScalaPlugin, ScalaPluginExtension}
import org.gradle.api.provider.Property
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.{ScalaSourceDirectorySet, SourceSet, SourceSetContainer, SourceTask, TaskProvider}
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.{Action, GradleException, Plugin, Project, Task, UnknownTaskException}
import org.gradle.jvm.tasks.Jar
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.util.internal.GUtil
import org.podval.tools.build.jvm.JvmBackend
import org.podval.tools.build.{AddConfigurationToClassPath, BackendDependencyRequirements, GradleClassPath,
  ScalaBackend, ScalaLibrary, ScalaVersion, Version}
import org.podval.tools.build.ScalaBackend.sharedSourceRoot
import org.podval.tools.platform.IntelliJIdea
import org.podval.tools.scalajsplugin.jvm.JvmDelegate
import org.podval.tools.scalajsplugin.scalajs.ScalaJSDelegate
import org.podval.tools.scalajsplugin.scalanative.ScalaNativeDelegate
import org.podval.tools.test.task.TestTask
import org.podval.tools.util.Files
import org.slf4j.Logger
import scala.jdk.CollectionConverters.*
import java.io.File
import java.lang.reflect.Field
import javax.inject.Inject

final class ScalaJSPlugin @Inject(jvmPluginServices: JvmPluginServices) extends Plugin[Project]:
  override def apply(project: Project): Unit =
    def pluginMessage(message: String): String = ScalaJSPlugin.pluginMessage(project, message)
    def lifecycle(message: String): Unit = project.getLogger.lifecycle(pluginMessage(message))
    def help(message: String): Unit = project.getLogger.lifecycle(ScalaJSPlugin.helpMessage(project, message))

    // Apply Scala plugin to this project.
    project.getPluginManager.apply(classOf[ScalaPlugin])

    // Create extension.
    project.getExtensions.create("scalaBackend", classOf[ScalaBackendExtension])

    val isRunningInIntelliJ: Boolean = IntelliJIdea.runningIn
    val ijString: String = if !isRunningInIntelliJ then "" else " [IJ]"

    // If Scala version property is set, override Scala version and adjust build directory.
    Option(project.findProperty(ScalaJSPlugin.scalaVersionProperty)).map(_.toString).foreach: (scalaVersion: String) =>
      val extensionScalaVersion: Property[String] = ScalaJSPlugin.getScalaExtensionScalaVersionProperty(project)
      lifecycle(s"using Scala $scalaVersion; build directory adjusted")
      // TODO it is to late to change the Scala version of the overall project it seems;
      // its Scala-related tasks are disabled, so as long as we do not propagate the wrong version, we are ok...
      extensionScalaVersion.set(scalaVersion)
      val buildDirectory: DirectoryProperty = project.getLayout.getBuildDirectory
      buildDirectory.set(buildDirectory.get.dir(s"scala-$scalaVersion"))

    def backendNames: String = ScalaBackend.all.map(backend => s"${backend.name} (${backend.sourceRoot})").mkString(", ")
    def sourceRoots: String = ScalaBackend.all.map(_.sourceRoot).mkString(", ")
    val backendCandidates: Set[ScalaBackend] = ScalaBackend.all.filter(backend => project.file(backend.sourceRoot).exists)
    val notSubprojects: Set[ScalaBackend] = backendCandidates.filterNot(ScalaJSPlugin.findBackendSubproject(project, _).isDefined)
    if notSubprojects.nonEmpty then project.getLogger.warn(pluginMessage(
      s"subprojects ${notSubprojects.map(_.sourceRoot).map(n => s"'$n'").mkString(", ")} must be included in 'settings.gradle'"
    ))

    val isModeMixed: Boolean = backendCandidates.nonEmpty
    if isModeMixed then
      // Calculate `includeShared`.
      val sharedExists: Boolean =
        val file: File = project.file(sharedSourceRoot)
        file.exists && file.isDirectory
      if !sharedExists then
        help(s"to share code between backends, create directory '$sharedSourceRoot'")
      val sharedNotAvailable: Boolean = sharedExists && isRunningInIntelliJ && ScalaJSPlugin.findSharedSubproject(project).isEmpty
      if sharedNotAvailable then
        help(s"for shared sources to be visible in IntelliJ IDE, include subproject '$sharedSourceRoot' in 'settings.gradle'")
      val includeShared: Boolean = sharedExists && !sharedNotAvailable

      // Write `settings-includes.gradle`.
      Files.write(
        file = File(project.getProjectDir, "settings-includes.gradle"),
        content = (Seq(sharedSourceRoot) ++ backendCandidates.map(_.sourceRoot))
          .map(name => s"include '${project.getProjectDir.getName}:$name'")
          .mkString("\n")
      )
      
      val backends: Set[ScalaBackend] = backendCandidates.filter(ScalaJSPlugin.findBackendSubproject(project, _).isDefined)
      lifecycle(s"using Scala backends ${backends.map(_.name).mkString(", ")}$ijString")
      ScalaJSPlugin.applyMixed(
        project,
        backends,
        includeShared = includeShared
      )
    else
      // TODO get from extension
      val delegateOpt: Option[BackendDelegate[?]] = Option(project.findProperty(ScalaJSPlugin.scalaBackendProperty))
        .map(_.toString)
        .map((name: String) => Set(JvmDelegate, ScalaJSDelegate, ScalaNativeDelegate)
          .find(_.backend.is(name))
          .getOrElse(throw GradleException(pluginMessage(s"unknown Scala backend '$name'; use one of $backendNames")))
        )

      if delegateOpt.isEmpty && backendCandidates.isEmpty then help(
        s"""to choose Scala backend, set property '${ScalaJSPlugin.scalaBackendProperty}' to one of $backendNames;
           |to share code between backends, create at least one of the subprojects $sourceRoots""".stripMargin
      )

      val delegate: BackendDelegate[?] = delegateOpt.getOrElse(JvmDelegate)

      val includeShared: Boolean = Option(project.findProperty(ScalaJSPlugin.includeSharedProperty))
        .map(_.toString)
        .contains("true")

      val sharedString: String = if !includeShared then "" else s" (including '$sharedSourceRoot')"
      lifecycle(s"using Scala backend ${delegate.backend.name}$sharedString$ijString")

      ScalaJSPlugin.applySingle(
        project,
        delegate,
        includeShared = includeShared,
        isRunningInIntelliJ = isRunningInIntelliJ,
        jvmPluginServices = jvmPluginServices
      )

object ScalaJSPlugin:
  private def pluginMessage(project: Project, message: String): String = s"Plugin 'org.podval.tools.scalajs' in $project: $message."
  def helpMessage(project: Project, message: String): String =
    s"${pluginMessage(project, message)}\nDocumentation: https://github.com/dubinsky/scalajs-gradle"

  val scalaVersionProperty : String = "org.podval.tools.scalajs.scalaVersion"
  val scalaBackendProperty : String = "org.podval.tools.scalajs.backend"
  val includeSharedProperty: String = "org.podval.tools.scalajs.includeShared"

  private def implementationConfigurationName: String = JvmConstants.IMPLEMENTATION_CONFIGURATION_NAME
  private def scalaCompilerPluginsConfigurationName: String = ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME
  private def testScalaCompilerPluginsConfigurationName: String = GUtil.toLowerCamelCase(s"test $scalaCompilerPluginsConfigurationName")

  // We look up projects by their *directory* names, not by their *project* names,
  // so `Option(project.findProject(name))` does not do it for projects renamed in `settings.gradle` ;)
  private def findSubproject(project: Project, name: String): Option[Project] = project
    .getSubprojects
    .asScala
    .find(_.getProjectDir.getName == name)

  private def findSharedSubproject(project: Project) = findSubproject(project, sharedSourceRoot)
  private def findBackendSubproject(project: Project, backend: ScalaBackend) = findSubproject(project, backend.sourceRoot)

  def getScalaExtensionScalaVersionProperty(project: Project): Property[String] = project
    .getExtensions
    .getByType(classOf[ScalaPluginExtension])
    .getScalaVersion

//  private def getScalaBackendExtensionNameProperty(project: Project): Property[String] = project
//    .getExtensions
//    .getByType(classOf[ScalaBackendExtension])
//    .getName

  private def setExtProperty(project: Project, name: String, value: Object): Unit = project
    .getExtensions
    .getByType(classOf[ExtraPropertiesExtension])
    .set(name, value)

  private def applyMixed(
    project: Project,
    backends: Set[ScalaBackend],
    includeShared: Boolean
  ): Unit =
    val sharedSubprojectOpt: Option[Project] = findSharedSubproject(project)

    // Disable `SourceTask` tasks of the overall project and unregister all its Scala sources.
    project.getTasks.withType(classOf[SourceTask]).configureEach(_.setEnabled(false))
    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(project)
    getScalaSourceDirectorySet(mainSourceSet).setSrcDirs(List.empty.asJava)
    getScalaSourceDirectorySet(testSourceSet).setSrcDirs(List.empty.asJava)

    // If the shared subproject exists, apply 'scala' plugin to it and disable all its tasks.
    sharedSubprojectOpt.foreach((sharedSubproject: Project) =>
      sharedSubproject.getPluginManager.apply("scala")
      sharedSubproject.getTasks.withType(classOf[Task]).configureEach(_.setEnabled(false))
    )

    val scalaVersionOpt: Option[ScalaVersion] = Option(getScalaExtensionScalaVersionProperty(project).getOrNull).map(ScalaVersion(_))
    for backend: ScalaBackend <- backends do
      // Configure backend and apply the plugin to the subprojects.
      val subproject: Project = findBackendSubproject(project, backend).get
      setExtProperty(subproject, scalaBackendProperty, backend.name)
      if includeShared then setExtProperty(subproject, includeSharedProperty, "true")
      subproject.getPluginManager.apply(classOf[ScalaJSPlugin])
//      ScalaJSPlugin.getScalaBackendExtensionNameProperty(subproject).set(backend.name)
      // TODO do we need the early propagation?
      scalaVersionOpt.foreach(scalaVersion => getScalaExtensionScalaVersionProperty(subproject).set(scalaVersion.toString))

    project.afterEvaluate: (project: Project) =>
      // TODO too late to propagate Scala version; delete
      // Set Scala version property on the subprojects and the shared project (if it exists).
      // TODO do we care to set Scala version for the shared project, where all the tasks are disabled?
      val subprojectsToSetScalaVersionOn: Seq[Project] =
        sharedSubprojectOpt.toSeq ++
        backends.map(findBackendSubproject(project, _)).map(_.get)

      // malchus of the overall project becomes kesser of the subproject
      val scalaVersion: String = scalaVersionOpt.getOrElse(ScalaLibrary
        .getFromConfiguration(project.getConfigurations.getByName(implementationConfigurationName))
        .scalaVersion).toString
      subprojectsToSetScalaVersionOn.foreach(getScalaExtensionScalaVersionProperty(_).set(scalaVersion))

  private def applySingle(
    project: Project,
    delegate: BackendDelegate[?],
    includeShared: Boolean,
    isRunningInIntelliJ: Boolean,
    jvmPluginServices: JvmPluginServices
  ): Unit =
    createTestScalaCompilerPluginsConfiguration(project, jvmPluginServices)
    configureTestTask(project)
    configureHasRuntimeClassPathTasks(project)
    configureDependsOnClassesTasks(project)

    delegate.backend.archiveAppendixOpt.foreach(configureArchiveAppendix(project, _))
    delegate.createExtension.foreach(_.create(project))
    delegate.pluginDependenciesConfigurationNameOpt.foreach(createPluginDependenciesConfiguration(project, _, delegate.backend.name))
    registerTasks(project, delegate)

    project.afterEvaluate: (project: Project) =>
      addSources(project, includeShared = includeShared, isRunningInIntelliJ = isRunningInIntelliJ)
      val projectScalaLibrary: ScalaLibrary = ScalaLibrary
        .getFromConfiguration(project.getConfigurations.getByName(implementationConfigurationName))
      // TODO verify that the library version is the same as the one in the extension (if it is set)
      val pluginScalaVersion: ScalaVersion = ScalaLibrary.getFromClasspath(GradleClassPath.collect(this)).scalaVersion
      dependencyRequirements(
        project,
        delegate,
        projectScalaLibrary.scalaVersion,
        pluginScalaVersion
      ).foreach(_.apply(project))
      configureJarTask(project, delegate, projectScalaLibrary.scalaVersion)
      configureScalaCompile(project, delegate, projectScalaLibrary.scalaVersion)
      expandClassPath(project, delegate, projectScalaLibrary)

  private def addSources(
    project: Project,
    includeShared: Boolean,
    isRunningInIntelliJ: Boolean
  ): Unit =
    val scalaRootsForVersions: Seq[String] = Option(getScalaExtensionScalaVersionProperty(project).getOrNull)
      .map(ScalaVersion(_))
      .map(_.version)
      .map(version => 1.to(version.length).map(version.take).map(version => s"scala-$version"))
      .getOrElse(Seq.empty)

    // Add non-shared version-specific Scala sources.
    addScalaSources(project, scalaRootsForVersions, sourceRoot = None)

    if includeShared then
      val scalaRoots: Seq[String] = Seq("scala") ++ scalaRootsForVersions
      if !isRunningInIntelliJ then
        addScalaSources(project, scalaRoots, sourceRoot = Some(ScalaBackend.sharedSourceRoot))
      else
        // Add dependency on the shared sibling.
        // TODO exclude this dependency from publications!
        val sharedSibling: Project = ScalaJSPlugin.findSharedSubproject(project.getParent).get
        project.getDependencies.add(implementationConfigurationName, sharedSibling)

        // Add shared sources for the execution of the tasks that need them.
        val addSharedScalaSourcesForTask: Action[Task] = ScalaJSPlugin.addSharedScalaSourcesForTask(project, scalaRoots)
        project.getTasks.withType(classOf[SourceTask]).configureEach(addSharedScalaSourcesForTask)
        project.getTasks.withType(classOf[AbstractArchiveTask]).configureEach(addSharedScalaSourcesForTask)

  private def getScalaSourceDirectorySet(sourceSet: SourceSet): ScalaSourceDirectorySet = sourceSet
    .getExtensions
    .getByType(classOf[ScalaSourceDirectorySet])

  private def addScalaSources(sourceSet: SourceSet, sourceDirectories: Seq[File]): Unit =
    getScalaSourceDirectorySet(sourceSet).srcDirs(sourceDirectories*)

  private val defaultSourceDirectorySetSource: Field = classOf[DefaultSourceDirectorySet].getDeclaredField("source")
  defaultSourceDirectorySetSource.setAccessible(true)

  private def removeScalaSources(sourceSet: SourceSet, sourceDirectories: Seq[File]): Unit =
    def toRemove(o: Object): Boolean = o.isInstanceOf[File] && {
      val file: File = o.asInstanceOf[File]
      sourceDirectories.exists(_.getAbsolutePath == file.getAbsolutePath)
    }

    val scalaSourceDirectorySet: ScalaSourceDirectorySet = getScalaSourceDirectorySet(sourceSet)
    scalaSourceDirectorySet.setSrcDirs(defaultSourceDirectorySetSource
      .get(scalaSourceDirectorySet)
      .asInstanceOf[java.util.List[Object]]
      .asScala
      .filterNot(toRemove)
      .asJava
    )

  private def scalaSources(
    project: Project,
    scalaRoots: Seq[String],
    isTest: Boolean,
    sourceRoot: Option[String]
  ): Seq[File] =
    for scalaRoot: String <- scalaRoots yield Files.fileSeq(
      (if sourceRoot.isEmpty then project else project.getParent).getProjectDir,
      sourceRoot.toSeq ++ Seq(
        "src",
        if isTest then "test" else "main",
        scalaRoot
      )
    )

  private def addScalaSources(
    project: Project,
    scalaRoots: Seq[String],
    sourceRoot: Option[String]
  ): Unit =
    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(project)
    addScalaSources(mainSourceSet, scalaSources(project, scalaRoots, isTest = false, sourceRoot))
    addScalaSources(testSourceSet, scalaSources(project, scalaRoots, isTest = true , sourceRoot))

  // Add before compilation and remove after, so that IntelliJ does not
  // run into "duplicate content roots" issue during project import.
  private def addSharedScalaSourcesForTask(
    project: Project,
    scalaRoots: Seq[String]
  ): Action[Task] = (task: Task) =>
    def sharedScalaSources(isTest: Boolean): Seq[File] = scalaSources(
      project,
      scalaRoots,
      isTest,
      sourceRoot = Some(ScalaBackend.sharedSourceRoot)
    )
    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(project)
    task.doFirst(new Action[Task]:
      override def execute(t: Task): Unit =
        addScalaSources(mainSourceSet, sharedScalaSources(isTest = false))
        addScalaSources(testSourceSet, sharedScalaSources(isTest = true ))
    )
    task.doLast(new Action[Task]:
      override def execute(t: Task): Unit =
        removeScalaSources(mainSourceSet, sharedScalaSources(isTest = false))
        removeScalaSources(testSourceSet, sharedScalaSources(isTest = true ))
    )

  private def configureArchiveAppendix(
    project: Project,
    archiveAppendix: String
  ): Unit =
    val archiveAppendixConvention: Action[Jar] = (jar: Jar) => jar
      .getArchiveAppendix
      .convention(project.provider(() => if jar.getArchiveClassifier.isPresent then archiveAppendix else null))

    project
      .getTasks
      .withType(classOf[Jar])
      .configureEach(archiveAppendixConvention)

  private def configureJarTask(
    project: Project,
    delegate: BackendDelegate[?],
    scalaVersion: ScalaVersion
  ): Unit =
    val jarTaskName: String = JvmConstants.JAR_TASK_NAME
    project.getTasks.withType(classOf[Jar]).named(jarTaskName).configure(
      removeDashBeforeArchiveAppendix(project)
    )

    val jarAppendix: String = s"${delegate.backend.artifactSuffixString}_${scalaVersion.binaryVersion.versionSuffix}"
    project.getTasks.withType(classOf[Jar]).named(jarTaskName).configure((jar: Jar) =>
      jar.getArchiveAppendix.convention(jarAppendix)
    )

  private def removeDashBeforeArchiveAppendix(project: Project): Action[Jar] = (jar: Jar) => jar
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

  private def configureTestTask(project: Project): Unit = project
    .getTasks
    .withType(classOf[TestTask])
    .configureEach((testTask: TestTask) =>
      testTask.setGroup(JavaBasePlugin.VERIFICATION_GROUP)
      testTask.useSbt()
    )

  private def getSourceSet(project: Project, isTest: Boolean): SourceSet =
    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(project)
    if isTest
    then testSourceSet
    else mainSourceSet

  private def configureHasRuntimeClassPathTasks(project: Project): Unit = project
    .getTasks
    .withType(classOf[BackendTask.HasRuntimeClassPath])
    .configureEach(task => task.getRuntimeClassPath.setFrom(getSourceSet(project, task.isTest).getRuntimeClasspath))

  private def configureDependsOnClassesTasks(project: Project): Unit = project
    .getTasks
    .withType(classOf[BackendTask.DependsOnClasses])
    .configureEach(task => task.dependsOn(getClassesTaskProvider(project, getSourceSet(project, task.isTest))))

  private def createPluginDependenciesConfiguration(
    project: Project,
    configurationName: String,
    backendName: String
  ): Unit =
    // TODO use new one-shot methods resolvable()/consumable() etc.
    val configuration: Configuration = project.getConfigurations.create(configurationName)
    configuration.setVisible(false)
    configuration.setCanBeConsumed(false)
    configuration.setDescription(s"$backendName dependencies used by the ScalaJS plugin.")

  private def createTestScalaCompilerPluginsConfiguration(
    project: Project,
    jvmPluginServices: JvmPluginServices
  ): Unit =
    val testPlugins: Configuration = project
      .asInstanceOf[ProjectInternal]
      .getConfigurations
      .resolvableDependencyScopeUnlocked(testScalaCompilerPluginsConfigurationName)
    testPlugins.setTransitive(false)
    jvmPluginServices.configureAsRuntimeClasspath(testPlugins)

  private def registerTasks(
    project: Project,
    delegate: BackendDelegate[?]
  ): Unit =
    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(project)
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
        task.setDescription(s"$before ${delegate.backend.name} code$after.")
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
    project: Project,
    delegate: BackendDelegate[?],
    projectScalaVersion: ScalaVersion,
    pluginScalaVersion: ScalaVersion
  ): Seq[ApplyDependencyRequirements] =
    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(project)
    val implementationConfigurationName    : String = mainSourceSet.getImplementationConfigurationName
    val testImplementationConfigurationName: String = testSourceSet.getImplementationConfigurationName
    val testRuntimeOnlyConfigurationName   : String = testSourceSet.getRuntimeOnlyConfigurationName

    val requirements: BackendDependencyRequirements = delegate.backend.dependencyRequirements(
      implementationConfiguration     = project.getConfigurations.getByName(implementationConfigurationName    ),
      testImplementationConfiguration = project.getConfigurations.getByName(testImplementationConfigurationName),
      scalaVersion = projectScalaVersion
    )

    delegate.pluginDependenciesConfigurationNameOpt.toSeq.map(
      ApplyDependencyRequirements(requirements.pluginDependencies      , pluginScalaVersion , _)
    ) ++ Seq(
      ApplyDependencyRequirements(requirements.implementation          , projectScalaVersion, implementationConfigurationName          ),
      ApplyDependencyRequirements(requirements.testRuntimeOnly         , projectScalaVersion, testRuntimeOnlyConfigurationName         ),
      ApplyDependencyRequirements(requirements.scalaCompilerPlugins    , projectScalaVersion, scalaCompilerPluginsConfigurationName    ),
      ApplyDependencyRequirements(requirements.testScalaCompilerPlugins, projectScalaVersion, testScalaCompilerPluginsConfigurationName)
    )

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

  private def configureScalaCompile(
    project: Project,
    delegate: BackendDelegate[?],
    scalaVersion: ScalaVersion
  ): Unit =
    val scalaCompileParameters: Seq[String] = delegate.backend.scalaCompileParameters(scalaVersion)
    def ensureParameters(scalaCompile: ScalaCompile): Unit =
      ScalaJSPlugin.ensureParameters(scalaCompile, scalaCompileParameters, project.getLogger)

    def addScalaCompilerPlugins(scalaCompile: ScalaCompile, configurationName: String): Unit =
      ScalaJSPlugin.addScalaCompilerPlugins(project.getConfigurations.getByName(configurationName), scalaCompile, project.getLogger)

    val (mainSourceSet: SourceSet, testSourceSet: SourceSet) = getSourceSets(project)

    val mainScalaCompile: ScalaCompile = ScalaJSPlugin.scalaCompile(project, mainSourceSet)
    ensureParameters(mainScalaCompile)
    addScalaCompilerPlugins(mainScalaCompile, scalaCompilerPluginsConfigurationName)

    val testScalaCompile: ScalaCompile = ScalaJSPlugin.scalaCompile(project, testSourceSet)
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

  private def getClassesTaskProvider(project: Project, sourceSet: SourceSet): TaskProvider[Task] = project
    .getTasks
    .named(sourceSet.getClassesTaskName)

  def findDependsOnProviderOrTask[T <: Task](task: Task, clazz: Class[? <: T]): Option[T] =
    findDependsOnTaskProvider(task, clazz)
      .map(_.get)
      .orElse(findDependsOnTask(task, clazz))

  private def findDependsOnTaskProvider[T <: Task](task: Task, clazz: Class[? <: T]): Option[TaskProvider[T]] = task
    .getDependsOn
    .asScala
    .filter(_.isInstanceOf[TaskProvider[?]])
    .filter(_.isInstanceOf[ProviderInternal[?]])
    .map(_.asInstanceOf[ProviderInternal[T]])
    .find(candidate => clazz.isAssignableFrom(candidate.getType))
    .map(_.asInstanceOf[TaskProvider[T]])

  private def findDependsOnTask[T <: Task](task: Task, clazz: Class[? <: T]): Option[T] = task
    .getDependsOn
    .asScala
    .filter(_.isInstanceOf[Task])
    .map(_.asInstanceOf[Task])
    .find(candidate => clazz.isAssignableFrom(candidate.getClass))
    .map(_.asInstanceOf[T])

  // TODO use task name!
  private def scalaCompile(project: Project, sourceSet: SourceSet): ScalaCompile = getClassesTaskProvider(project, sourceSet)
    .get
    .getDependsOn
    .asScala
    .find(classOf[TaskProvider[ScalaCompile]].isInstance)
    .get
    .asInstanceOf[TaskProvider[ScalaCompile]]
    .get

  private def getSourceSets(project: Project): (SourceSet, SourceSet) =
    val sourceSets: SourceSetContainer = project.getExtensions.getByType(classOf[JavaPluginExtension]).getSourceSets
    (
      sourceSets.getByName(JvmConstants      .JAVA_MAIN_FEATURE_NAME ),
      sourceSets.getByName(JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME)
    )
