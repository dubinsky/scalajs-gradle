package org.podval.tools.testing

import org.testng.annotations.Test

class TestProjectsTest:
  import TestProject.*

  // TODO I double-discover nested suites!!!
  /*
  --- TEST RESULTS ---
  23: org.podval.tools.testing.JUnit4Test.failure failed=1 skipped=0
    22: org.podval.tools.testing.JUnit4Test.failure resultType=FAILURE
  37: org.podval.tools.testing.JUnit4Test.testAssertSame failed=0 skipped=0
    36: org.podval.tools.testing.JUnit4Test.testAssertSame resultType=SUCCESS
  15: org.podval.tools.testing.JUnit4Test.testAssertNotNull failed=0 skipped=0
    14: org.podval.tools.testing.JUnit4Test.testAssertNotNull resultType=SUCCESS
  39: org.podval.tools.testing.JUnit4Test.testAssertTrue failed=0 skipped=0
    38: org.podval.tools.testing.JUnit4Test.testAssertTrue resultType=SUCCESS
  35: org.podval.tools.testing.JUnit4Test.testAssertNull failed=0 skipped=0
    34: org.podval.tools.testing.JUnit4Test.testAssertNull resultType=SUCCESS
  27: org.podval.tools.testing.JUnit4Test.testAssertThatBothContainsString failed=0 skipped=0
    26: org.podval.tools.testing.JUnit4Test.testAssertThatBothContainsString resultType=SUCCESS
  17: org.podval.tools.testing.JUnit4Test.testAssertNotSame failed=0 skipped=0
    16: org.podval.tools.testing.JUnit4Test.testAssertNotSame resultType=SUCCESS
  21: org.podval.tools.testing.JUnit4Test.testAssertThatEveryItemContainsString failed=0 skipped=0
    20: org.podval.tools.testing.JUnit4Test.testAssertThatEveryItemContainsString resultType=SUCCESS
  33: org.podval.tools.testing.JUnit4Test.testAssertThatHamcrestCoreMatchers failed=0 skipped=0
    32: org.podval.tools.testing.JUnit4Test.testAssertThatHamcrestCoreMatchers resultType=SUCCESS
  29: org.podval.tools.testing.JUnit4Test.testAssertEquals failed=0 skipped=0
    28: org.podval.tools.testing.JUnit4Test.testAssertEquals resultType=SUCCESS
  31: org.podval.tools.testing.JUnit4Test.testAssertArrayEquals failed=0 skipped=0
    30: org.podval.tools.testing.JUnit4Test.testAssertArrayEquals resultType=SUCCESS
  25: org.podval.tools.testing.JUnit4Test.testAssertFalse failed=0 skipped=0
    24: org.podval.tools.testing.JUnit4Test.testAssertFalse resultType=SUCCESS
  13: org.podval.tools.testing.JUnit4Test failed=0 skipped=0
  19: org.podval.tools.testing.JUnit4Test.testAssertThatHasItems failed=0 skipped=0
    18: org.podval.tools.testing.JUnit4Test.testAssertThatHasItems resultType=SUCCESS

  49: org.podval.tools.testing.MUnitTest failed=0 skipped=0
  48: org.podval.tools.testing.MUnitTest.2=2 failed=0 skipped=0
    47: org.podval.tools.testing.MUnitTest.2=2 resultType=SUCCESS
  46: org.podval.tools.testing.MUnitTest.42 != 43 failed=1 skipped=0
    45: org.podval.tools.testing.MUnitTest.42 != 43 resultType=FAILURE
  */
  @Test def frameworks(): Unit =
    forProject("frameworks",
      forClass(className = "org.podval.tools.testing.ScalaTestTest", failed=1, skipped=1,
        passed("2*2 success should pass"),
        failed("2*2 failure should fail"),
        skipped("2*2 failure should be ignored")
      ),

      forClass("org.podval.tools.testing.ScalaCheckTest", failed=1, skipped=0,
        passed("String.startsWith"),
        failed("String.concatenate"),
        passed("String.substring")
      ),

      forClass("org.podval.tools.testing.Spec2Test", failed=1, skipped=0,
        passed("contain 11 characters"),
        passed("start with 'Hello'"),
        passed("end with 'world'"),
        failed("fail to end with 'xxx'")
      ),

      forClass("org.podval.tools.testing.UTestTest", failed=2, skipped=0,
        failed("test1"),
        passed("test2"),
        failed("test3"),
      ),

      forClass(className = "org.podval.tools.testing.JUnit4Test.testAssertThatHasItems", failed = 0, skipped = 0,
        passed("org.podval.tools.testing.JUnit4Test.testAssertThatHasItems")
      ),
    )

  @Test def scalaOnly(): Unit =
    forProject("scala-only",
      forClass(className = "org.podval.tools.testing.ExampleSpec", failed = 0, skipped = 0,
        passed("The Scala language must add correctly")
      ),
      forClass(className = "org.podval.tools.testing.StackSpec", failed = 0, skipped = 1,
        passed("A Stack should pop values in last-in-first-out order"),
        skipped("A Stack should throw NoSuchElementException if an empty stack is popped")
      ),
      forClass(className = "org.podval.tools.testing.TwoByTwoTest", failed = 1, skipped = 0,
        passed("2*2 success should work"),
        passed("3*3 success should work"),
        failed("2*2 failure should fail")
      )
    )

  @Test def tutorial212(): Unit =
    forProject("tutorial-Scala2.12",
      forClass(className = "tutorial.webapp.TutorialTest", failed = 1, skipped = 0,
        failed("HelloWorld failure")
      ),
      forClass(className = "tutorial.webapp.TutorialTest2", failed = 1, skipped = 0,
        passed("2*2 success")
      )
    )

  @Test def tutorial213(): Unit =
    forProject("tutorial-Scala2.13",
      forClass(className = "tutorial.webapp.TutorialTest", failed = 1, skipped = 0,
        failed("HelloWorld failure")
      ),
      forClass(className = "tutorial.webapp.TutorialTest2", failed = 1, skipped = 0,
        passed("2*2 success")
      )
    )

  @Test def tutorial3(): Unit =
    forProject("tutorial-Scala3",
      forClass(className = "tutorial.webapp.TutorialTest", failed = 1, skipped = 0,
        failed("HelloWorld failure")
      ),
      forClass(className = "tutorial.webapp.TutorialTest2", failed = 1, skipped = 0,
        passed("2*2 success")
      )
    )
