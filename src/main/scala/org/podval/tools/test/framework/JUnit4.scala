package org.podval.tools.test.framework

import org.podval.tools.build.{DependencyMaker, JavaDependencyMaker, ScalaBackend, Version}
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
object JUnit4 extends FrameworkDescriptor(
  name = "JUnit",
  description = "JUnit4",
  group = "com.github.sbt",
  artifact = "junit-interface",
  className = "com.novocode.junit.JUnitFramework",
  sharedPackages = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit"),
  tagOptionStyle = OptionStyle.ListWithEq, 
  includeTagsOption = "--include-categories", 
  excludeTagsOption = "--exclude-categories",
  // by default, `org.junit.runners.Suite` is ignored; make sure it is not: it is needed to run nested suites:
  additionalOptions = Array("--ignore-runners=none"),
  usesTestSelectorAsNestedTestSelector = true
) with JavaDependencyMaker:
  override val versionDefault: Version = Version("0.13.3")

  // This is a JVM-only test framework
  override def maker(backend: ScalaBackend): Option[DependencyMaker] = backend match
    case JvmBackend => Some(this)
    case _ => None

  override def underlying(backend: ScalaBackend): Option[DependencyMaker] = backend match
    case JvmBackend => Some(JUnit4Underlying)
    case _ => None
