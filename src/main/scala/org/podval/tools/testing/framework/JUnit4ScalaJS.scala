package org.podval.tools.testing.framework

import org.podval.tools.build.Version
import org.podval.tools.testing.worker.TestTagsFilter

// https://github.com/scala-js/scala-js/tree/main/junit-runtime/src/main/scala
// https://github.com/scala-js/scala-js/blob/main/junit-runtime/src/main/scala/org/scalajs/junit/JUnitFramework.scala
// https://github.com/nicolasstucki/scala-js-junit
// Dependencies:
// ScalaJS:
object JUnit4ScalaJS extends FrameworkDescriptor(
  name = "Scala.js JUnit test framework",
  displayName = "JUnit4 ScalaJS",
  group = "org.scala-js",
  artifact = "scalajs-junit-test-runtime",
  versionDefault = Version("1.18.2"), // Note: Scala.js version!
  className = "com.novocode.junit.JUnitFramework",
  sharedPackages = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit"),
):
  override def isJvmSupported: Boolean = false
  override def isScalaJSSupported: Boolean = true

  override def isScalaDependency: Boolean = true
  override def isScala2OnlyDependency: Boolean = true
  override def isJvmOnlyDependency: Boolean = true

  override def args(testTagsFilter: TestTagsFilter): Seq[String] =
    FrameworkDescriptor.listOption("--include-categories", testTagsFilter.include) ++
    FrameworkDescriptor.listOption("--exclude-categories", testTagsFilter.exclude)
