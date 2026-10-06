package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaTestFramework, Version}

// https://github.com/typelevel/scalacheck
// https://github.com/typelevel/scalacheck/blob/main/core/shared/src/main/scala/org/scalacheck/ScalaCheckFramework.scala
object ScalaCheck extends ScalaTestFramework(
  name = "ScalaCheck",
  nameSbt = "ScalaCheck",
  group = "org.scalacheck",
  artifact = "scalacheck",
  versionDefault = Version("1.20.0"),
  className = "org.scalacheck.ScalaCheckFramework",
  sharedPackages = List("org.scalacheck")
)
