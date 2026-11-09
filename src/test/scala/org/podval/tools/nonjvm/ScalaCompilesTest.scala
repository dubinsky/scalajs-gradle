package org.podval.tools.nonjvm

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ScalaCompilesTest extends AnyFlatSpec, Matchers:
  "java output version" should "be added for a Scala 3.8+ compile that does not set one" in:
    ScalaCompiles.javaOutputVersionToAdd(Seq("-scalajs"), scala3LibraryCompiledByScala3 = true) shouldBe
      Some(ScalaCompiles.javaOutputVersionParameter)

  it should "be left alone when the build already sets a release" in:
    ScalaCompiles.javaOutputVersionToAdd(Seq("-release:21"), scala3LibraryCompiledByScala3 = true) shouldBe None
    ScalaCompiles.javaOutputVersionToAdd(Seq("-java-output-version:21"), scala3LibraryCompiledByScala3 = true) shouldBe None

  it should "not be added for Scala 2 or Scala 3.7" in:
    ScalaCompiles.javaOutputVersionToAdd(Seq.empty, scala3LibraryCompiledByScala3 = false) shouldBe None
