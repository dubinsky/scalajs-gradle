name := "Scala.js Tutorial using sbt"

scalaVersion := "3.1.3"

enablePlugins(ScalaJSPlugin)

// This is an application with a main method
scalaJSUseMainModuleInitializer := true

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.2.0"
jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()

libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.14" % "test"
//testFrameworks += new TestFramework("org.scalatest.tools.Framework")
