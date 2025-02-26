package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaDependency, Version}

// http://etorreborre.github.io/specs2/
// https://github.com/etorreborre/specs2
// https://github.com/etorreborre/specs2/blob/main/core/shared/src/main/scala/org/specs2/runner/Specs2Framework.scala
// Specs (org.specs.runner.SpecsFramework) is deprecated; use Specs2.

// Dependencies:
// Scala:
// org.specs2:specs2-core_3:5.5.8
//   org.specs2:specs2-common_3
//   org.specs2:specs2-matcher_3
//   org.specs2:specs2-fp_3
//   org.portable-scala:portable-scala-reflect_2.13
//   org.scala-sbt:test-interface:1.0
// also:
//   org.scala-lang:scala3-library_3
//   org.scala-lang:scala-library:2.13.x
//
// ScalaJS:
// org.specs2:specs2-core_sjs1_3:5.5.8
//   org.specs2:specs2-common_sjs1_3
//   org.specs2:specs2-matcher_sjs1_3
//   org.specs2:specs2-fp_sjs1_3
//   org.portable-scala:portable-scala-reflect_sjs1_2.13
//   org.scala-js:scala-js-macrotask-executor_sjs1_3
//   org.scala-sbt:test-interface:1.0
//   org.scala-js:scalajs-test-interface_2.13
// also:
//   org.scala-lang:scala-library:2.13.x

object Specs2 extends FrameworkDescriptor(
  name = "specs2",
  displayName = "Spec2",
  group = "org.specs2",
  artifact = "specs2-core",
  versionDefault = Version("5.5.8"),
  className = "org.specs2.runner.Specs2Framework",
  sharedPackages = List("org.specs2.runner")
) with ScalaDependency.Maker:
  override def args(
    includeTags: Set[String],
    excludeTags: Set[String]
  ): Seq[String] =
    FrameworkDescriptor.listOptionNoEq("include", includeTags) ++
    FrameworkDescriptor.listOptionNoEq("exclude", excludeTags)
