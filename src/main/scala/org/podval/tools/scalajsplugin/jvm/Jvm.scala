package org.podval.tools.scalajsplugin.jvm

import org.gradle.api.tasks.{SourceSet, TaskProvider}
import org.gradle.api.{Project, Task}
import org.podval.tools.build.{Gradle, JavaDependency, ScalaBackendKind, ScalaPlatform, Version}
import org.podval.tools.scalajsplugin.{BackendDelegate, GradleNames}

object Jvm extends BackendDelegate:
  override def testTaskClass: Class[JvmTestTask] = classOf[JvmTestTask]

  override def backendKind: ScalaBackendKind = ScalaBackendKind.JVM
  override def sourceRoot: String = "jvm"
  override def pluginDependenciesConfigurationNameOpt: Option[String] = None
  override def createExtensions(project: Project): Unit = ()
  override def scalaCompileParameters(isScala3: Boolean): Seq[String] = Seq.empty

  override def addTasks(
    project: Project,
    gradleNames: GradleNames
  ): Option[TaskProvider[? <: Task]] =
    project.getTasks.withType(classOf[JvmRunTask]).configureEach((task: JvmRunTask) =>
      task.setDescription(s"Runs ${backendKind.displayName} code.")
      task.setGroup("other")
      val sourceSet: SourceSet = Gradle.getSourceSet(project, gradleNames.mainSourceSetName)
      task.dependsOn(Gradle.getClassesTaskProvider(project, sourceSet))
      task.getRuntimeClassPath.setFrom(sourceSet.getRuntimeClasspath)
    )

    project.getTasks.register(
      gradleNames.runTaskName,
      classOf[JvmRunTask]
    )
    
    None

  override def applyDependencyRequirements(
    project: Project,
    gradleNames: GradleNames,
    pluginScalaPlatform: ScalaPlatform,
    projectScalaPlatform: ScalaPlatform,
    isScala3: Boolean
  ): Unit =
    BackendDelegate.applyDependencyRequirements(
      project,
      Seq(Jvm.SbtTestInterface.required()),
      projectScalaPlatform,
      Gradle.getSourceSet(project, gradleNames.testSourceSetName).getImplementationConfigurationName
    )
  
  object SbtTestInterface extends JavaDependency.Maker:
    override def group: String = "org.scala-sbt"
    override def artifact: String = "test-interface"
    override def versionDefault: Version = Version("1.0")
    override def description: String = "SBT testing interface; some test frameworks (ScalaTest :)) do not bring it in in."
