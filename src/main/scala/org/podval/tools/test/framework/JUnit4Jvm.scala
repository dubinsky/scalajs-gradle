package org.podval.tools.test.framework

import org.podval.tools.build.{Backend, JavaDependency, Version}
import org.podval.tools.jvm.JvmBackend

// https://github.com/sbt/junit-interface
// https://github.com/sbt/junit-interface/blob/develop/src/main/java/com/novocode/junit/JUnitFramework.java
// https://github.com/junit-team/junit4
// https://github.com/junit-team/junit4/blob/main/src/main/java/org/junit/experimental/categories/Category.java
//
// No nested tasks.
// Dependencies:
// Scala:
// com.github.sbt:junit-interface
//   junit:junit:4.13.2
//   org.hamcrest:hamcrest-core
//   org.scala-sbt:test-interface:1.0
object JUnit4Jvm extends Framework(
  name = "JUnit4",
  nameSbt = "JUnit",
  className = "com.novocode.junit.JUnitFramework",
  sharedPackages = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit"),
  tagOptions = TagOptions.ListWithEq("--include-categories", "--exclude-categories"),
  usesTestSelectorAsNested = true,
  additionalOptions = Array(
    // by default, `org.junit.runners.Suite` is ignored; make sure it is not - it is needed to run nested suites:
    "--ignore-runners=none",
    "--summary=1" // enable one-line summary
  )
):
  override def dependency: JavaDependency = JavaDependency(
    name = name,
    group = "com.github.sbt",
    versionDefault = Version("0.13.3"),
    artifact = "junit-interface"
  )

  override def isBackendSupported(backend: Backend): Boolean = backend == JvmBackend

  // JUnit4 itself does not contain an sbt test framework runner;
  // `com.github.sbt:junit-interface` supplied one (see `JUni4`);
  // `MUnit` also uses underlying JUni4 - via its own internal interface.
  object Underlying extends JavaDependency(
    name = "Underlying JUnit4 Test Framework",
    group = "junit",
    versionDefault = Version("4.13.2"),
    artifact = "junit"
)
