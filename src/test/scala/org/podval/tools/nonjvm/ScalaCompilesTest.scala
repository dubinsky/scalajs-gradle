package org.podval.tools.nonjvm

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ScalaCompilesTest extends AnyFlatSpec, Matchers:
  "java release" should "be added for a Scala 3.8+ compile that does not set one" in:
    ScalaCompiles.javaReleaseToAdd(Seq("-scalajs"), scala3LibraryCompiledByScala3 = true) shouldBe
      Some(ScalaCompiles.javaReleaseParameter)

  it should "be left alone when the build already sets a release" in:
    ScalaCompiles.javaReleaseToAdd(Seq("-release:21"), scala3LibraryCompiledByScala3 = true) shouldBe None
    ScalaCompiles.javaReleaseToAdd(Seq("-java-output-version:21"), scala3LibraryCompiledByScala3 = true) shouldBe None

  it should "not be added for Scala 2 or Scala 3.7" in:
    ScalaCompiles.javaReleaseToAdd(Seq.empty, scala3LibraryCompiledByScala3 = false) shouldBe None
