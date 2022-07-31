package org.podval.tools.test

class MUnitTest extends munit.FunSuite:
  test("hello") {
    val obtained = 42
    val expected = 43
    assertEquals(obtained, expected)
  }
