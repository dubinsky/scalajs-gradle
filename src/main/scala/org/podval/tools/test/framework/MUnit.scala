package org.podval.tools.test.framework

import org.podval.tools.build.{DependencyMaker, ScalaBackend, ScalaVersion, Version}
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.scalajs.ScalaJSBackend
import org.podval.tools.scalanative.ScalaNativeBackend

// https://scalameta.org/munit/
// https://github.com/scalameta/munit/blob/main/munit/jvm/src/main/scala/munit/Framework.scala
// https://github.com/scalameta/munit/blob/main/munit/non-jvm/src/main/scala/munit/Framework.scala
// https://github.com/scalameta/munit/blob/main/junit-interface/src/main/java/munit/internal/junitinterface/JUnitFramework.java

// Dependencies:
// Scala:
// org.scalameta:munit_3
//   org.scalameta:munit-diff_3
//   org.scalameta:junit-interface
//   junit:junit:4.13.2
//   org.scala-sbt:test-interface:1.0
// also:
//   org.scala-lang:scala3-library_3
//
// Scala.js:
// org.scalameta:munit_sjs1_3
//   org.scalameta:munit-diff_sjs1_3
//   org.scala-js:scalajs-junit-test-runtime_2.13
//   org.scala-js:scalajs-test-interface_2.13
// also:
//   org.scala-lang:scala3-library_sjs1_3
//   org.scala-js:scalajs-library_2.13
//   org.scala-lang:scala-library:2.13.x
object MUnit extends ScalaFrameworkDescriptor(
  name = "munit",
  description = "MUnit",
  group = "org.scalameta",
  artifact = "munit",
  versionDefault = Version("1.1.1"),
  className = "munit.Framework",
  sharedPackages = List("munit"),
  tagOptions = TagOptions.ListWithEq("--include-tags", "--exclude-tags"),
  usesTestSelectorAsNestedTestSelector = true
):
  override def additionalOptions: Array[String] = Array(
    "--logger=sbt", // use SBT loggers
    "--summary=1" // enable one-line summary
  )

  override def underlying(backend: ScalaBackend): Option[DependencyMaker] = backend match
    case JvmBackend         => JUnit4           .underlying(backend)
    case ScalaJSBackend     => JUnit4ScalaJS    .forBackend(backend)
    case ScalaNativeBackend => JUnit4ScalaNative.forBackend(backend)
