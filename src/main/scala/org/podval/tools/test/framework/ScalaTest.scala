package org.podval.tools.test.framework

import org.podval.tools.test.TestTagsFilter

// implementation: https://github.com/scalatest/scalatest/blob/main/jvm/core/src/main/scala/org/scalatest/tools/Framework.scala
// runner arguments: https://www.scalatest.org/user_guide/using_the_runner
// Note: no nested tasks
object ScalaTest extends FrameworkDescriptor(
  name = "ScalaTest",
  implementationClassName = "org.scalatest.tools.Framework"
):
  override def args(
    testTagsFilter: TestTagsFilter
  ): Array[String] =
    def option(name: String, values: Seq[String]): Seq[String] =
      values.flatMap((value: String) => Seq(name, value))

    val result: Seq[String] =
      option("-n", testTagsFilter.include.toSeq) ++
      option("-l", testTagsFilter.exclude.toSeq)

    result.toArray
