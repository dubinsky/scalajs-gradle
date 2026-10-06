package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaTestFramework, TagOptions, Version}

// https://scalameta.org/munit/
// https://github.com/scalameta/munit/blob/main/munit/jvm/src/main/scala/munit/Framework.scala
// https://github.com/scalameta/munit/blob/main/munit/non-jvm/src/main/scala/munit/Framework.scala
// https://github.com/scalameta/munit/blob/main/junit-interface/src/main/java/munit/internal/junitinterface/JUnitFramework.java
object MUnit extends ScalaTestFramework(
  name = "MUnit",
  nameSbt = "munit",
  group = "org.scalameta",
  artifact = "munit",
  className = "munit.Framework",
  sharedPackages = List("munit"),
  tagOptions = TagOptions.ListWithEq("--include-tags", "--exclude-tags"),
  usesTestSelectorAsNested = true,
  versionDefault = Version("1.3.6"),
  additionalOptions = Array(
    "--logger=sbt", // use SBT loggers
    "--summary=1" // enable one-line summary
  )
):
  override def additionalOptions(isJvm: Boolean): Array[String] =
    if isJvm
    then additionalOptions // from 1.2.4, JVM-only options cause exception when not running on JVM
    else Array.empty
