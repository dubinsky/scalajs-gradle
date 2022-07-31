package org.podval.tools.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

final class TwoByTwoTest extends AnyFlatSpec, Matchers:

  "2*2 success" should "work" in {
    2*2 shouldBe 4
  }

  "3*3 success" should "work" in {
    println("--- SCALA-ONLY TEST OUTPUT ---")
    3*3 shouldBe 9
  }

  "2*2 failure" should "fail" in {
    2*2 shouldBe 5
  }
