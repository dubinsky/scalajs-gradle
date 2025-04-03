package org.podval.tools.scalajsplugin

import org.gradle.api.tasks.TaskProvider
import org.gradle.api.{Action, Project}
import org.podval.tools.build.Gradle
import org.podval.tools.test.task.TestTask

final class TestTaskMaker[T <: TestTask](
  testSourceSetName: String,
  clazz: Class[T],
  configure: T => Unit
):
  private def doConfigure(project: Project, testTask: T): Unit =
    testTask.dependsOn(Gradle.getClassesTask(project, testSourceSetName))
    configure(testTask)

  def replace(project: Project, name: String): T =
    val result: T = project.getTasks.replace(name, clazz)
    doConfigure(project, result)
    result
  
  def register(project: Project, name: String): TaskProvider[T] =
    project.getTasks.register(
      name, 
      clazz,
      new Action[T]:
        override def execute(testTask: T): Unit = doConfigure(project, testTask)
    )
