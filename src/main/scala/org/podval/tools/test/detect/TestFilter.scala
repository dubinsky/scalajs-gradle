package org.podval.tools.test.detect

// Note: since there is no way to find out what tests are defined in a given suite,
// I need to know the meaning of the inclusion patterns (and can't handle individual test exclusions);
// I also do not need `def matchesTest(className: String, methodName: String): Boolean` method.

// Note: based on org.gradle.api.internal.tasks.testing.filter.TestSelectionMatcher
// see https://github.com/gradle/gradle/blob/master/subprojects/testing-base/src/main/java/org/gradle/api/internal/tasks/testing/filter/TestSelectionMatcher.java
final class TestFilter(
  includes: TestFilterPatterns,
  excludes: TestFilterPatterns,
  commandLineIncludes: TestFilterPatterns
):
  def matchClass(className: String): Option[TestFilterMatch] =
    // Note: any exclusion of even one method excludes the class
    //      excludes.exists(pattern =>pattern.patternMatches(pattern.determineTargetClassName(className)))
    val excluded: Boolean = !excludes.isEmpty && excludes          .matchClass(className).isDefined
    if excluded then None else
      for
        included           : TestFilterMatch <- includes           .matchClass(className)
        commandLineIncluded: TestFilterMatch <- commandLineIncludes.matchClass(className)
        intersection: TestFilterMatch = included.intersect(commandLineIncluded)
        if !intersection.isEmpty
      yield intersection

object TestFilter:
  def apply(
    includes: Set[String],
    excludes: Set[String],
    commandLineIncludes: Set[String]
  ): TestFilter =
    def toPatterns(strings: Set[String]): TestFilterPatterns = TestFilterPatterns(strings.map(TestFilterPattern(_)))
    new TestFilter(
      includes = toPatterns(includes),
      excludes = toPatterns(excludes),
      commandLineIncludes = toPatterns(commandLineIncludes)
    )
