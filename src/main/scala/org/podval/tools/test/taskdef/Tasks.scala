package org.podval.tools.test.taskdef

import sbt.testing.{Selector, Task, TaskDef}

object Tasks:
  def toString(task: Task): String = TaskDefs.toString(task.taskDef)
  
  def getSelector(task: Task): Selector =
    val taskDef: TaskDef = task.taskDef
    require(taskDef.selectors.length == 1, "Exactly one Selector is required")
    taskDef.selectors()(0)
