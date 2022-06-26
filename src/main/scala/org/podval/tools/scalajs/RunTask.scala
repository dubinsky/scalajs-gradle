package org.podval.tools.scalajs

// Note: base on org.scalajs.sbtplugin.ScalaJSPluginInternal;
// see https://github.com/scala-js/scala-js/blob/main/sbt-plugin/src/main/scala/org/scalajs/sbtplugin/ScalaJSPluginInternal.scala
abstract class RunTask[T <: LinkTask](clazz: Class[T]) extends AfterLinkTask[T](clazz):
  final override protected def flavour: String = "Run"

  final override protected def doExecute(actions: Actions): Unit = actions.run()

object RunTask:
  class FastOpt       extends RunTask(classOf[LinkTask.Main.FastOpt      ]) with ScalaJSTask.FastOpt
  class FullOpt       extends RunTask(classOf[LinkTask.Main.FullOpt      ]) with ScalaJSTask.FullOpt
  class FromExtension extends RunTask(classOf[LinkTask.Main.FromExtension]) with ScalaJSTask.FromExtension
