package org.podval.tools.test.framework

import org.podval.tools.scalanative.ScalaNativeBackend

object JUnit4ScalaNative extends NonJvmJUnit4Framework(
  artifact = "junit-runtime",
  name = "Scala Native JUnit test framework",
  className = "scala.scalanative.junit.JUnitFramework",
  sharedPackages = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit")
):
  override def supportedBackend: ScalaNativeBackend.type = ScalaNativeBackend
  override def scalaBackend: ScalaNativeBackend.type = ScalaNativeBackend
