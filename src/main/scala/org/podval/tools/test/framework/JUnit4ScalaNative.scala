package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackend, ScalaDependencyMaker, Version}
import org.podval.tools.scalanative.{ScalaNativeBackend, ScalaNativeDependency}

object JUnit4ScalaNative extends FrameworkDescriptor(
  name = "Scala Native JUnit test framework",
  description = "JUnit4 Scala Native",
  group = ScalaNativeDependency.group,
  artifact = "junit-runtime",
  className = "scala.scalanative.junit.JUnitFramework",
  sharedPackages = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit"),
  usesTestSelectorAsNestedTestSelector = true
) with ScalaFrameworkDescriptor:
  override def versionDefault: Version = ScalaNativeDependency.versionDefault

  // This is a Scala Native-only test framework
  override def maker(backend: ScalaBackend): Option[ScalaDependencyMaker] = backend match
    case ScalaNativeBackend => super.maker(backend)
    case _ => None
