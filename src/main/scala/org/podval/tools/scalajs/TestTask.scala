package org.podval.tools.scalajs

abstract class TestTask[T <: LinkTask.Test](clazz: Class[T]) extends AfterLinkTask[T](clazz):
  final override protected def flavour: String = "Test"

  final override protected def doExecute(actions: Actions): Unit = actions.test()

object TestTask:
  class FastOpt       extends TestTask(classOf[LinkTask.Test.FastOpt      ]) with ScalaJSTask.FastOpt
  class FullOpt       extends TestTask(classOf[LinkTask.Test.FullOpt      ]) with ScalaJSTask.FullOpt
  class FromExtension extends TestTask(classOf[LinkTask.Test.FromExtension]) with ScalaJSTask.FromExtension
