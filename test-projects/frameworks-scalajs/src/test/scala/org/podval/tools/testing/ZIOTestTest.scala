package org.podval.tools.testing

import zio.test.*
import zio.Scope

object ZIOTestTest extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] = suite("some suite")(
    test("failing test") {
      assert(1)(Assertion.equalTo(2))
    },
    test("passing test") {
      assert(1)(Assertion.equalTo(1))
    },
    test("failing test assertTrue") {
      val one = 1
      assertTrue(one == 2)
    },
    test("passing test assertTrue") {
      assertTrue(1 == 1)
    }
  )
}
