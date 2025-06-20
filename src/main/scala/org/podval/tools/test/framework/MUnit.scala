package org.podval.tools.test.framework

import org.podval.tools.backend.jvm.JvmBackend
import org.podval.tools.backend.scalajs.ScalaJSBackend
import org.podval.tools.backend.scalanative.ScalaNativeBackend
import org.podval.tools.build.{DependencyMaker, ScalaBackend, Version}

// https://scalameta.org/munit/
// https://github.com/scalameta/munit/blob/main/munit/jvm/src/main/scala/munit/Framework.scala
// https://github.com/scalameta/munit/blob/main/munit/non-jvm/src/main/scala/munit/Framework.scala
// https://github.com/scalameta/munit/blob/main/junit-interface/src/main/java/munit/internal/junitinterface/JUnitFramework.java

// Dependencies:
// Scala:
// org.scalameta:munit_3:1.1.0
//   org.scalameta:munit-diff_3:1.1.0
//   org.scalameta:junit-interface:1.1.0
//   junit:junit:4.13.2
//   org.scala-sbt:test-interface:1.0
// also:
//   org.scala-lang:scala3-library_3
//
// Scala.js:
// org.scalameta:munit_sjs1_3:1.0.4
//   org.scalameta:munit-diff_sjs1_3:1.0.4
//   org.scala-js:scalajs-junit-test-runtime_2.13
//   org.scala-js:scalajs-test-interface_2.13
// also:
//   org.scala-lang:scala3-library_sjs1_3
//   org.scala-js:scalajs-library_2.13
//   org.scala-lang:scala-library:2.13.x
object MUnit extends FrameworkDescriptor(
  name = "munit",
  displayName = "MUnit",
  group = "org.scalameta",
  artifact = "munit",
  versionDefault = Version("1.1.1"),
  className = "munit.Framework",
  sharedPackages = List("munit"),
  tagOptionStyle = OptionStyle.ListWithEq, 
  includeTagsOption = "--include-tags", 
  excludeTagsOption = "--exclude-tags",
  // use SBT loggers
  additionalOptions = Array("--logger=sbt"),
  usesTestSelectorAsNestedTestSelector = true
):
  private def forBackend(backend: ScalaBackend, underlying: DependencyMaker): Some[ForBackend] = Some(ForBackend(
    maker = ScalaMaker(backend),
    underlying = Some(underlying)
  ))
  
  // on JVM, uses underlying JUni4 - via its own internal interface
  override val forJVM   : Some[ForBackend] = forBackend(JvmBackend, JUnit4Underlying)
  // on Scala.js, uses JUnit4 for Scala.js - with its own sbt.testing.Framework implementation
  override val forJS    : Some[ForBackend] = forBackend(ScalaJSBackend, JUnit4ScalaJS.forJS.get.maker)
  override val forNative: Some[ForBackend] = forBackend(ScalaNativeBackend, JUnit4ScalaNative.forNative.get.maker)
