package org.podval.tools.test.framework

import org.podval.tools.build.{DependencyMaker, ScalaBackend, Version}
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.scalajs.ScalaJSBackend

// https://github.com/scala-js/scala-js/tree/main/junit-runtime/src/main/scala
// https://github.com/scala-js/scala-js/blob/main/junit-runtime/src/main/scala/org/scalajs/junit/JUnitFramework.scala
// https://github.com/nicolasstucki/scala-js-junit
object JUnit4ScalaJS extends FrameworkDescriptor(
  name = "Scala.js JUnit test framework",
  displayName = "JUnit4 Scala.js",
  group = "org.scala-js", 
  artifact = "scalajs-junit-test-runtime",
  className = "com.novocode.junit.JUnitFramework",
  sharedPackages = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit"),
  usesTestSelectorAsNestedTestSelector = true
):
  override def versionDefault: Version = ScalaJSBackend.versionDefault

  // This is a Scala.js-only test framework
  override def maker(backend: ScalaBackend): Option[DependencyMaker] = backend match
    case ScalaJSBackend => Some(
      new ScalaMaker(JvmBackend):
        override def isPublishedForScala3: Boolean = false
    )
    case _ => None
