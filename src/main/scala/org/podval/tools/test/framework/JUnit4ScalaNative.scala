package org.podval.tools.test.framework

import org.podval.tools.build.{DependencyMaker, ScalaBackend, Version}
import org.podval.tools.scalanative.ScalaNativeBackend

object JUnit4ScalaNative extends FrameworkDescriptor(
  name = "Scala Native JUnit test framework",
  displayName = "JUnit4 Scala Native",
  group = "org.scala-native",
  artifact = "junit-runtime",
  className = "scala.scalanative.junit.JUnitFramework",
  sharedPackages = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit"),
  usesTestSelectorAsNestedTestSelector = true
):
  override def versionDefault: Version = ScalaNativeBackend.versionDefault

  // This is a Scala Native-only test framework
  override def maker(backend: ScalaBackend): Option[DependencyMaker] = backend match
    case ScalaNativeBackend => super.maker(backend)
    case _ => None
