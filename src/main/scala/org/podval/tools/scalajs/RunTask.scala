package org.podval.tools.scalajs

import org.gradle.api.{DefaultTask, GradleException}
import org.gradle.api.tasks.TaskAction
import org.podval.tools.testing.task.{SourceMapper, TestEnvironment, TestTask}
import scala.jdk.CollectionConverters.*

trait RunTask extends ScalaJSTask:
  protected def linkTaskClass: Class[? <: LinkTask]

  protected final def linkTask: LinkTask = getDependsOn
    .asScala
    .find((candidate: AnyRef) => linkTaskClass.isAssignableFrom(candidate.getClass))
    .map(_.asInstanceOf[LinkTask])
    .getOrElse(throw GradleException(s"Task $getName must depend on a task of type ${linkTaskClass.getName}!"))

object RunTask:
  class Main extends DefaultTask with RunTask:
    setGroup("other")
    final override protected def flavour: String = "Run"
    final override protected def linkTaskClass: Class[LinkTask.Main] = classOf[LinkTask.Main]

    @TaskAction final def execute(): Unit =
      setUpNodeProject()
      ScalaJS(task = this, linkTask).run()

  class Test extends TestTask with RunTask:
    final override protected def flavour: String = "Test"
    final override protected def linkTaskClass: Class[LinkTask.Test] = classOf[LinkTask.Test]
    // Note: ScalaJS tests are not forkable; see org.scalajs.sbtplugin.ScalaJSPluginInternal
    final override protected def canFork: Boolean = false

    private var scalaJS: Option[ScalaJS] = None
    @TaskAction override def executeTests(): Unit =
      setUpNodeProject()
      scalaJS = Some(ScalaJS(task = this, linkTask))
      super.executeTests()
      scalaJS = None

    final override protected def sourceMapper: Option[SourceMapper] = scalaJS.get.sourceMapper
    final override protected def testEnvironment: TestEnvironment = scalaJS.get.testEnvironment
