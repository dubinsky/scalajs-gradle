package org.podval.tools.test.detect

// Note: since there is no way to find out what tests are defined in a given suite,
// I need to know the meaning of the inclusion patterns (and can't handle individual test exclusions).

// Note: based on org.gradle.api.internal.tasks.testing.filter.TestSelectionMatcher
// see https://github.com/gradle/gradle/blob/master/subprojects/testing-base/src/main/java/org/gradle/api/internal/tasks/testing/filter/TestSelectionMatcher.java
final class TestFilter(
  includes: TestFilterPatterns,
  excludes: TestFilterPatterns,
  commandLineIncludes: TestFilterPatterns
):
  def mayIncludeClass(className: String): Boolean = matchesClass(className).nonEmpty

  def matchesClass(className: String): Option[TestFilterMatch] =
    // Note: any exclusion of even one method excludes the class
    //      excludes.exists(pattern =>pattern.patternMatches(pattern.determineTargetClassName(className)))
    val excluded: Boolean = !excludes.isEmpty && excludes          .matches(className).isDefined
    if excluded then None else
      for
        included           : TestFilterMatch <- includes           .matches(className)
        commandLineIncluded: TestFilterMatch <- commandLineIncludes.matches(className)
        intersection: TestFilterMatch = included.intersect(commandLineIncluded)
        if !intersection.isEmpty
      yield intersection

//  def matchesTest(className: String, methodName: String): Boolean =
//    def matchesClassAndMethod(patterns: Set[TestPattern]): Boolean = patterns.exists(pattern =>
//      pattern.patternMatches(pattern.determineTargetClassName(className)) ||
//      (
//        (methodName != null) &&
//        pattern.patternMatches(pattern.determineTargetClassName(className) + "." + methodName)
//      )
//    )
//
//    // When there is a class name match, return true for excluding it so that we can keep
//    // searching in individual test methods for an exact match. If we return false here
//    // instead, then we'll give up searching individual test methods and just ignore the
//    // entire test class, which may not be what we want.
//    (buildScriptIncludePatterns.isEmpty || matchesClassAndMethod(buildScriptIncludePatterns)) &&
//    (commandLineIncludePatterns.isEmpty || matchesClassAndMethod(commandLineIncludePatterns)) &&
//    (buildScriptExcludePatterns.isEmpty || (
//      !matchesClassAndMethod(buildScriptExcludePatterns) &&
//      (matches(buildScriptExcludePatterns, className).isEmpty || (methodName != null))
//    ))

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
