package org.podval.tools.testing

class MUnitTest extends munit.FunSuite:
  test("42 != 43") {
    val obtained = 42
    val expected = 43
    assertEquals(obtained, expected)
  }

  test("2=2") {
    assertEquals(2, 2)
  }
