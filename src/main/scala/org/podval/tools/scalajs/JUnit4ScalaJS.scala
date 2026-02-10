package org.podval.tools.scalajs

import org.podval.tools.nonjvm.NonJvmJUnit4TestFramework
import org.podval.tools.scalajs.ScalaJSBackend

// https://github.com/scala-js/scala-js/tree/main/junit-runtime/src/main/scala
// https://github.com/scala-js/scala-js/blob/main/junit-runtime/src/main/scala/org/scalajs/junit/JUnitFramework.scala
// https://github.com/nicolasstucki/scala-js-junit
object JUnit4ScalaJS extends NonJvmJUnit4TestFramework(
  backend = ScalaJSBackend,
  transform = _.scala2.jvm,
  artifact = "scalajs-junit-test-runtime",
  nameSbt = "Scala.js JUnit test framework",
  className = "com.novocode.junit.JUnitFramework",
  sharedPackages = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit")
)
