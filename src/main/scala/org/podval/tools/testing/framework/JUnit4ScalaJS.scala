package org.podval.tools.testing.framework

import org.podval.tools.build.{ScalaDependency, Version}
import org.podval.tools.testing.worker.TestTagsFilter

// https://github.com/scala-js/scala-js/tree/main/junit-runtime/src/main/scala
// https://github.com/scala-js/scala-js/blob/main/junit-runtime/src/main/scala/org/scalajs/junit/JUnitFramework.scala
// https://github.com/nicolasstucki/scala-js-junit
// Dependencies:
// ScalaJS:
// Note: does not support included/excluded categories options
object JUnit4ScalaJS extends FrameworkDescriptor(
  name = "Scala.js JUnit test framework",
  displayName = "JUnit4 ScalaJS",
  group = "org.scala-js",
  artifact = "scalajs-junit-test-runtime",
  versionDefault = Version("1.18.2"), // Note: Scala.js version!
  className = "com.novocode.junit.JUnitFramework",
  sharedPackages = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit"),
) with ScalaDependency.MakerScala2Jvm:
  // This is a Scala.js-only test framework
  override protected def isJvmSupported: Boolean = false

  // TODO https://github.com/dubinsky/scalajs-gradle/issues/36
  override protected def isScalaJSSupported: Boolean = false

  // This framework does not support test tagging.
  override def args(testTagsFilter: TestTagsFilter): Seq[String] = Seq.empty
