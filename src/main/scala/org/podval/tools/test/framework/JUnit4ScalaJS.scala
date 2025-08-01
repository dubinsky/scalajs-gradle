package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackend, ScalaDependency, ScalaVersion}
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
) with ScalaDependency:
  override def supportedBackend: ScalaJSBackend.type = ScalaJSBackend
  override def scalaBackend: JvmBackend.type = JvmBackend
  override def isPublishedFor(scalaVersion: ScalaVersion): Boolean = scalaVersion.isScala2

  // it is a JVM dependency!
  override def withBackend(backend: ScalaBackend): ScalaDependency =
    require(isBackendSupported(backend))
    this
