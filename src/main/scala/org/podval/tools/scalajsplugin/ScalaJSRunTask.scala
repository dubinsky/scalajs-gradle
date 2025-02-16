package org.podval.tools.scalajsplugin

import org.gradle.api.tasks.TaskAction
import org.gradle.api.{DefaultTask, GradleException}
import org.podval.tools.scalajs.ScalaJSActions
import org.podval.tools.testing.task.{SourceMapper, TestEnvironment, TestTask}
import scala.jdk.CollectionConverters.SetHasAsScala

trait ScalaJSRunTask extends ScalaJSTask:
  protected def linkTaskClass: Class[? <: ScalaJSLinkTask]

  final override protected def linkTask: ScalaJSLinkTask = getDependsOn
    .asScala
    .find((candidate: AnyRef) => linkTaskClass.isAssignableFrom(candidate.getClass))
    .map(_.asInstanceOf[ScalaJSLinkTask])
    .getOrElse(throw GradleException(s"Task $getName must depend on a task of type ${linkTaskClass.getName}!"))

object ScalaJSRunTask:
  abstract class Main extends DefaultTask with ScalaJSRunTask:
    setGroup("other")
    final override protected def flavour: String = "Run"
    final override protected def linkTaskClass: Class[ScalaJSLinkTask.Main] = classOf[ScalaJSLinkTask.Main]

    @TaskAction final def execute(): Unit = scalaJSActions.run()

  abstract class Test extends TestTask with ScalaJSRunTask:
    final override protected def flavour: String = "Test"
    final override protected def linkTaskClass: Class[ScalaJSLinkTask.Test] = classOf[ScalaJSLinkTask.Test]
    // Note: ScalaJS tests are not forkable; see org.scalajs.sbtplugin.ScalaJSPluginInternal
    final override protected def canFork: Boolean = false

    // cache for the call-backs used during execution
    private var scalaJSActionsCached: Option[ScalaJSActions] = None
    final override protected def sourceMapper: Option[SourceMapper] = scalaJSActionsCached.get.sourceMapper
    final override protected def testEnvironment: TestEnvironment = scalaJSActionsCached.get.testEnvironment

    @TaskAction override def executeTests(): Unit =
      scalaJSActionsCached = Some(scalaJSActions)
      super.executeTests()
      scalaJSActionsCached = None
