package org.podval.tools.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ScalaTestTest extends AnyFlatSpec, Matchers:
  "2*2 success" should "pass" in {
    2 * 2 shouldBe 4
  }

  "2*2 failure" should "fail" in {
    2 * 2 shouldBe 5
  }

  ignore should "be ignored" in {
    2 * 2 shouldBe 5
  }
