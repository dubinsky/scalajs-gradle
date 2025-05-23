package org.podval.tools.test.framework

import org.podval.tools.build.scalajs.ScalaJSBackend
import org.podval.tools.build.ScalaDependency

// https://github.com/scala-js/scala-js/tree/main/junit-runtime/src/main/scala
// https://github.com/scala-js/scala-js/blob/main/junit-runtime/src/main/scala/org/scalajs/junit/JUnitFramework.scala
// https://github.com/nicolasstucki/scala-js-junit
object JUnit4ScalaJS extends FrameworkDescriptor(
  name = "Scala.js JUnit test framework",
  displayName = "JUnit4 Scala.js",
  group = "org.scala-js",
  artifact = "scalajs-junit-test-runtime",
  versionDefault = ScalaJSBackend.versionDefault,
  className = "com.novocode.junit.JUnitFramework",
  sharedPackages = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit"),
  usesTestSelectorAsNestedTestSelector = true,
  // This is a Scala.js-only test framework
  forJVM = ForBackend.notSupported,
  forNative = ForBackend.notSupported
) with ScalaDependency.MakerScala2Jvm
