package org.podval.tools.test.framework

import org.podval.tools.jvm.JvmBackend
import org.podval.tools.scalajs.ScalaJSBackend

// https://github.com/scala-js/scala-js/tree/main/junit-runtime/src/main/scala
// https://github.com/scala-js/scala-js/blob/main/junit-runtime/src/main/scala/org/scalajs/junit/JUnitFramework.scala
// https://github.com/nicolasstucki/scala-js-junit
object JUnit4ScalaJS extends NonJvmJUnit4Framework(
  artifact = "scalajs-junit-test-runtime",
  name = "Scala.js JUnit test framework",
  className = "com.novocode.junit.JUnitFramework",
  sharedPackages = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit")
):
  override def supportedBackend: ScalaJSBackend.type = ScalaJSBackend
  override def scalaBackend: JvmBackend.type = JvmBackend
  override def isJvm: Boolean = true
  override def isPublishedForScala3: Boolean = false
