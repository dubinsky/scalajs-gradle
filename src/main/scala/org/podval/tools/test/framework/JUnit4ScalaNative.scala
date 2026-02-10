package org.podval.tools.test.framework

import org.podval.tools.scalanative.ScalaNativeBackend

object JUnit4ScalaNative extends NonJvmJUnit4Framework(
  backend = ScalaNativeBackend,
  transform = identity,
  artifact = "junit-runtime",
  nameSbt = "Scala Native JUnit test framework",
  className = "scala.scalanative.junit.JUnitFramework",
  sharedPackages = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit")
)
