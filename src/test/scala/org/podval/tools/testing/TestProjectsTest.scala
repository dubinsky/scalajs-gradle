package org.podval.tools.testing

import org.testng.annotations.Test

class TestProjectsTest:
  import TestProject.*

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

      forClass(className = "org.podval.tools.testing.JUnit4Test", failed = 1, skipped = 0,
        passed("testAssertNotNull"),
        passed("testAssertNotSame"),
        passed("testAssertThatHasItems"),
        failed("failure"),
      ),

      forClass(className = "org.podval.tools.testing.MUnitTest", failed = 1, skipped = 0,
        failed("42 != 43"),
        passed("2=2"),
      ),

      forClass(className = "org.podval.tools.testing.ZIOTestTest", failed = 2, skipped = 0,
        passed("some suite - passing test"),
        passed("some suite - passing test assertTrue"),
        failed("some suite - failing test"),
        failed("some suite - failing test assertTrue"),
      )
    )

  @Test def frameworksScalaJS(): Unit =
    forProject("frameworks-scalajs",
      forClass(className = "org.podval.tools.testing.ScalaTestTest", failed = 1, skipped = 1,
        passed("2*2 success should pass"),
        failed("2*2 failure should fail"),
        skipped("2*2 failure should be ignored")
      ),

      forClass("org.podval.tools.testing.ScalaCheckTest", failed = 1, skipped = 0,
        passed("String.startsWith"),
        failed("String.concatenate"),
        passed("String.substring")
      ),

      forClass("org.podval.tools.testing.Spec2Test", failed = 1, skipped = 0,
        passed("contain 11 characters"),
        passed("start with 'Hello'"),
        passed("end with 'world'"),
        failed("fail to end with 'xxx'")
      ),

      forClass("org.podval.tools.testing.UTestTest", failed = 2, skipped = 0,
        failed("test1"),
        passed("test2"),
        failed("test3"),
      ),

      forClass(className = "org.podval.tools.testing.MUnitTest", failed = 1, skipped = 0,
        failed("42 != 43"),
        passed("2=2"),
      ),

      // TODO no events from ZIO Test!
//      forClass(className = "org.podval.tools.testing.ZIOTestTest", failed = 2, skipped = 0,
//        passed("some suite - passing test"),
//        passed("some suite - passing test assertTrue"),
//        failed("some suite - failing test"),
//        failed("some suite - failing test assertTrue"),
//      )
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
