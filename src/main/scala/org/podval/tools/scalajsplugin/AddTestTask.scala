package org.podval.tools.scalajsplugin

import org.gradle.api.tasks.TaskProvider
import org.gradle.api.{Action, Project}
import org.podval.tools.build.Gradle
import org.podval.tools.test.task.TestTask

final class AddTestTask[T <: TestTask](
  clazz: Class[? <: T],
  configure: T => Unit
):
  def addTestTask(
    isModeMixed: Boolean, 
    project: Project,
    testSourceSetName: String,
    testTaskName: String
  ): Unit =
    def doConfigure(testTask: T): Unit =
      testTask.dependsOn(Gradle.getClassesTask(project, testSourceSetName))
      configure(testTask)

    if !isModeMixed
    then
      doConfigure(project.getTasks.replace("test", clazz))
    else
      // TODO add dependency on the backend test task to the overall test task
      project.getTasks.register(
        testTaskName,
        clazz,
        new Action[T]:
          override def execute(testTask: T): Unit = doConfigure(testTask)
      )
