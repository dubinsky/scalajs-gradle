package org.podval.tools.test.framework

import org.podval.tools.backend.jvm.JvmBackend
import org.podval.tools.backend.scalajs.ScalaJSBackend
import org.podval.tools.build.ScalaDependencyMaker

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
  usesTestSelectorAsNestedTestSelector = true
):
  override val forJS    : Some[ForBackend] = Some(ForBackend(
    new Maker with ScalaDependencyMaker:
      final override def scalaBackend: JvmBackend.type = JvmBackend
      final override def isPublishedForScala3: Boolean = false
  ))
  // This is a Scala.js-only test framework
  override val forJVM   : None.type = None
  override val forNative: Option[Nothing] = None
