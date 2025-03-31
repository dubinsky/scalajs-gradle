package org.podval.tools.test.framework

import org.podval.tools.build.{JavaDependency, Version}

// https://github.com/sbt/junit-interface
// https://github.com/sbt/junit-interface/blob/develop/src/main/java/com/novocode/junit/JUnitFramework.java
// https://github.com/junit-team/junit4
// https://github.com/junit-team/junit4/blob/main/src/main/java/org/junit/experimental/categories/Category.java
//
// No nested tasks.
// Dependencies:
// Scala:
// com.github.sbt:junit-interface:0.13.3
//   junit:junit:4.13.2
//   org.hamcrest:hamcrest-core
//   org.scala-sbt:test-interface:1.0
object JUnit4 extends FrameworkDescriptor(
  name = "JUnit",
  displayName = "JUnit4",
  group = "com.github.sbt",
  artifact = "junit-interface",
  versionDefault = Version("0.13.3"),
  className = "com.novocode.junit.JUnitFramework",
  sharedPackages = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit"),
  tagOptionStyle = OptionStyle.ListWithEq, 
  includeTagsOption = "--include-categories", 
  excludeTagsOption = "--exclude-categories",
  // by default, `org.junit.runners.Suite` is ignored; make sure it is not: it is needed to run nested suites:
  additionalOptions = Array("--ignore-runners=none"),
  includesClassNameInTestName = true,
  // This is a JVM-only test framework
  isScalaJSSupported = false,
  jvmUnderlying = Some(JUnit4Underlying)
) with JavaDependency.Maker
