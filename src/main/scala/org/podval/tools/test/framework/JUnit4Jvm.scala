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
// com.github.sbt:junit-interface
//   junit:junit:4.13.2
//   org.hamcrest:hamcrest-core
//   org.scala-sbt:test-interface:1.0
object JUnit4Jvm extends Framework with JavaDependency:
  override val group: String = "com.github.sbt"
  override val artifact: String = "junit-interface"
  override val versionDefault: Version = Version("0.13.3")
  override val description: String = "JUnit4"
  override val name: String = "JUnit"
  override val className: String = "com.novocode.junit.JUnitFramework"
  override val sharedPackages: List[String] = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit")
  override val tagOptions: Some[TagOptions] = TagOptions.ListWithEq("--include-categories", "--exclude-categories")
  override val usesTestSelectorAsNested: Boolean = true

  override def additionalOptions: Array[String] = Array(
    // by default, `org.junit.runners.Suite` is ignored; make sure it is not - it is needed to run nested suites:
    "--ignore-runners=none",
    "--summary=1" // enable one-line summary
  )

  // JUnit4 itself does not contain an sbt test framework runner;
  // `com.github.sbt:junit-interface` supplied one (see `JUni4`);
  // `MUnit` also uses underlying JUni4 - via its own internal interface.
  object Underlying extends JavaDependency:
    override val group: String = "junit"
    override val artifact: String = "junit"
    override val versionDefault: Version = Version("4.13.2")
    override val description: String = "Underlying JUnit4 Test Framework."
