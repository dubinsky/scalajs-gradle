package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackendKind, ScalaDependency}

object JUnit4ScalaNative extends FrameworkDescriptor(
  name = "Scala Native JUnit test framework",
  displayName = "JUnit4 Scala Native",
  group = "org.scala-native",
  artifact = "junit-runtime",
  versionDefault = ScalaBackendKind.Native.versionDefault,
  className = "scala.scalanative.junit.JUnitFramework",
  sharedPackages = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit"),
  usesTestSelectorAsNestedTestSelector = true,
  // This is a Scala Native-only test framework
  forJVM = ForBackend.notSupported,
  forJS = ForBackend.notSupported
) with ScalaDependency.Maker

