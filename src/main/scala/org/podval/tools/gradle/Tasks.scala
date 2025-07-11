package org.podval.tools.gradle

import org.gradle.api.internal.tasks.JvmConstants
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.{Action, Project, Task}

object Tasks:
  def verificationGroup: String = JavaBasePlugin.VERIFICATION_GROUP // LifecycleBasePlugin.VERIFICATION_GROUP
  def buildGroup: String = "build"
  def otherGroup: String = "other"

  def jarTaskName: String = JvmConstants.JAR_TASK_NAME

  // Test task and test source set are named the same.
  def testTaskName(project: Project): String = Configurations.testSourceSet(project).getName

  def taskName(project: Project, name: String, isTest: Boolean): String =
    Configurations.sourceSet(project, isTest).getTaskName(name, "")

  def disable[T <: Task](project: Project, taskClass: Class[T]): Unit = configureEach(
    project,
    taskClass,
    _.setEnabled(false)
  )

  def conventionProvider[T <: Task](
    task: T,
    property: T => Property[String],
    convention: T => String,
    project: Project
  ): Unit =
    property(task).convention(project.provider[String](() => convention(task)))

  def convention[T <: Task](
    task: T,
    property: T => Property[String],
    convention: T => String
  ): Unit =
    property(task).convention(convention(task))
  
  def configureEach[T <: Task](
    project: Project,
    taskClass: Class[T],
    action: Action[T]
  ): Unit = project
    .getTasks
    .withType(taskClass)
    .configureEach(action)

  def configure[T <: Task](
    project: Project,
    taskClass: Class[T],
    taskName: String,
    action: Action[T]
  ): Unit = project
    .getTasks
    .withType(taskClass)
    .named(taskName)
    .configure(action)

  def register[T <: Task](
    project: Project,
    taskClass: Class[T],
    taskName: String,
    description: String,
    group: String,
    dependsOn: Option[TaskProvider[?]] = None,
    replace: Boolean = false
  ): TaskProvider[T] =
    configureEach[T](
      project, 
      taskClass,
      (task: T) =>
        task.setDescription(description)
        task.setGroup(group)
    )

    val action: Action[T] = (task: T) => dependsOn.foreach(task.dependsOn(_))

    if !replace then
      project.getTasks.register(taskName, taskClass, action)
    else
      project.getTasks.replace(taskName, taskClass)
      project.getTasks.withType(taskClass).named(taskName, action)
