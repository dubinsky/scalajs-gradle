package org.podval.tools.test

import utest._

object UTestTest /* HelloTests */ extends TestSuite:
  val tests: Tests = Tests {
    test("test1") {
      throw new Exception("test1")
    }
    test("test2") {
      1
    }
    test("test3") {
      val a = List[Byte](1, 2)
      a(10)
    }
  }
