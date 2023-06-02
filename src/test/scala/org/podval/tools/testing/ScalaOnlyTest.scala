package org.podval.tools.testing

import ForClass.*

class ScalaOnlyTest extends GroupingFunSpec:
  describe("scala-only"):
    test(
      project = Memo(TestProject.existingProject("scala-only")),
      checks = Seq(
        forClass(className = "ExampleSpec",
          failedCount(0),
          skippedCount(0),
          passed("The Scala language must add correctly")
        ),
        forClass(className = "StackSpec",
          failedCount(0),
          skippedCount(1),
          passed("A Stack should pop values in last-in-first-out order"),
          skipped("A Stack should throw NoSuchElementException if an empty stack is popped")
        ),
        forClass(className = "TwoByTwoTest",
          failedCount(1),
          skippedCount(0),
          passed("2*2 success should work"),
          passed("3*3 success should work"),
          failed("2*2 failure should fail")
        )
      ),
      commandLineIncludeTestNames = Seq.empty
    )

