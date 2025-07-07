package org.podval.tools.test.framework

import org.podval.tools.build.Version

// implementation: https://github.com/scalatest/scalatest/blob/main/jvm/core/src/main/scala/org/scalatest/tools/Framework.scala
// runner arguments: https://www.scalatest.org/user_guide/using_the_runner
// No nested tasks.
// DOES NOT bring in test-interface (in non-Scala.js variant)!

// Dependencies:
// Scala:
// org.scalatest:scalatest_3
//   org.scalatest:scalatest-core_3
//     org.scalactic:scalactic_3
//     org.scala-lang.modules:scala-xml_3
//     org.scalatest:scalatest-compatible
//  and other ScalaTest modules:
//    org.scalatest:scalatest-featurespec_3
//    org.scalatest:scalatest-flatspec_3
//    org.scalatest:scalatest-freespec_3
//    org.scalatest:scalatest-funsuite_3
//    org.scalatest:scalatest-funspec_3
//    org.scalatest:scalatest-propspec_3
//    org.scalatest:scalatest-refspec_3
//    org.scalatest:scalatest-wordspec_3
//    org.scalatest:scalatest-diagrams_3
//    org.scalatest:scalatest-matchers-core_3
//    org.scalatest:scalatest-shouldmatchers_3
//    org.scalatest:scalatest-mustmatchers_3
// also:
//   org.scala-lang:scala3-library_3
//
// Scala.js:
// org.scalatest:scalatest_sjs1_3
//   org.scalatest:scalatest-core_sjs1_3
//     org.scalactic:scalactic_sjs1_3
//     org.scala-lang.modules:scala-xml_sjs1_3
//     org.scala-js:scalajs-test-interface_2.13
//  and other ScalaTest modules:
//    org.scalatest:scalatest-featurespec_sjs1_3
//    org.scalatest:scalatest-flatspec_sjs1_3
//    org.scalatest:scalatest-freespec_sjs1_3
//    org.scalatest:scalatest-funsuite_sjs1_3
//    org.scalatest:scalatest-funspec_sjs1_3
//    org.scalatest:scalatest-propspec_sjs1_3
//    org.scalatest:scalatest-refspec_sjs1_3
//    org.scalatest:scalatest-wordspec_sjs1_3
//    org.scalatest:scalatest-diagrams_sjs1_3
//    org.scalatest:scalatest-matchers-core_sjs1_3
//    org.scalatest:scalatest-shouldmatchers_sjs1_3
//    org.scalatest:scalatest-mustmatchers_sjs1_3
// also:
//   org.scala-lang:scala3-library_sjs1_3
//   org.scala-js:scalajs-library_2.13

object ScalaTest extends FrameworkDescriptor(
  name = "ScalaTest",
  displayName = "ScalaTest",
  group = "org.scalatest",
  artifact = "scalatest",
  className = "org.scalatest.tools.Framework",
  sharedPackages = List("org.scalatest"),
  tagOptionStyle = OptionStyle.OptionPerValue,
  includeTagsOption = "-n",
  excludeTagsOption = "-l"
):
  override val versionDefault: Version = Version("3.2.19")

