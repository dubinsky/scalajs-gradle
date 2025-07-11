package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackend, ScalaDependencyMaker, Version}
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.scalajs.{ScalaJSBackend, ScalaJSDependency}

// https://github.com/scala-js/scala-js/tree/main/junit-runtime/src/main/scala
// https://github.com/scala-js/scala-js/blob/main/junit-runtime/src/main/scala/org/scalajs/junit/JUnitFramework.scala
// https://github.com/nicolasstucki/scala-js-junit
object JUnit4ScalaJS extends FrameworkDescriptor(
  name = "Scala.js JUnit test framework",
  description = "JUnit4 Scala.js",
  group = ScalaJSDependency.group,
  artifact = "scalajs-junit-test-runtime",
  className = "com.novocode.junit.JUnitFramework",
  sharedPackages = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit"),
  usesTestSelectorAsNestedTestSelector = true
) with ScalaFrameworkDescriptor:
  override def versionDefault: Version = ScalaJSDependency.versionDefault

  // This is a Scala.js-only test framework
  override def maker(backend: ScalaBackend): Option[ScalaDependencyMaker] = backend match
    case ScalaJSBackend => Some(
      new ScalaDependencyMaker.Delegating(JUnit4ScalaJS.this) with ScalaDependencyMaker.NotPublishedForScala3:
        override def scalaBackend: ScalaBackend = JvmBackend
    )
    case _ => None
