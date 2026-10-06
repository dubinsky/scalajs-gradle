package org.podval.tools.test.filter

// Include/exclude combination follows Gradle's ClassTestSelectionMatcher (Gradle 9.8).
// An empty include set matches everything. Excluding any method of a class drops the class.
final class TestFilter(
  includes: TestFilterPatterns,
  excludes: TestFilterPatterns,
  commandLineIncludes: TestFilterPatterns
):
  def matchClass(className: String): Option[TestFilterMatch] =
    // Any exclusion of even one method excludes the class
    //   excludes.exists(pattern => pattern.patternMatches(pattern.determineTargetClassName(className)))
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
