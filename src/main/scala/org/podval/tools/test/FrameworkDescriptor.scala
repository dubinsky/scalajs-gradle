package org.podval.tools.test

import sbt.testing.Framework

// Note: based on sbt.TestFramework from org.scala-sbt.testing

// Note: add whatever other test frameworks are supported by ScalaJS
//  uTest
//  MiniTest
//  Greenlight
//  Scala Test-State
//  AirSpec
//
//  Nyaya
//  Scalaprops
class FrameworkDescriptor(
  val name: String,
  val implementationClassName: String
) derives CanEqual:

  def args(
    includeTags: Array[String],
    excludeTags: Array[String],
    isRemote: Boolean
  ): Array[String] = Array.empty

object FrameworkDescriptor:

  val all: List[FrameworkDescriptor] = List(
    ScalaTest,
    Specs,
    Specs2,
    JUnit
  )

  def forFramework(framework: Framework): FrameworkDescriptor = all
    .find(_.name == framework.name)
    .getOrElse(throw new IllegalArgumentException(s"Test framework descriptor for '${framework.name}' not found"))

//  object ScalaCheck extends FrameworkDescriptor(
//    "org.scalacheck.ScalaCheckFramework"
//  )

  object ScalaTest extends FrameworkDescriptor(
    name = "ScalaTest",
    implementationClassName = "org.scalatest.tools.Framework"
    // not an SBT framework: "org.scalatest.tools.ScalaTestFramework"
  ):
    override def args(
      includeTags: Array[String],
      excludeTags: Array[String],
      isRemote: Boolean
    ): Array[String] =
      def option(name: String, value: Seq[String]): Seq[String] =
        if value.isEmpty then Seq.empty else Seq(name, value.mkString(","))

      (option("-n", includeTags.toSeq) ++ option("-l", excludeTags.toSeq)).toArray

  object Specs extends FrameworkDescriptor(
    name = "Specs",
    implementationClassName = "org.specs.runner.SpecsFramework"
  )

  object Specs2 extends FrameworkDescriptor(
    name = "Specs2",
//    "org.specs2.runner.Specs2Framework", // TODO which one is the right one?
    implementationClassName = "org.specs2.runner.SpecsFramework"
  )

  object JUnit extends FrameworkDescriptor(
    name = "JUnit",
    implementationClassName = "com.novocode.junit.JUnitFramework"
  )

//  object MUnit extends FrameworkDescriptor(
//    "munit.Framework"
//  )
