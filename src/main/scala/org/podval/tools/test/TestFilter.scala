package org.podval.tools.test

// Note: since there is no way to find out what tests are defined in a given suite,
// I need to know the meaning of the inclusion patterns (and can't handle individual test exclusions).

// Note: based on org.gradle.api.internal.tasks.testing.filter.TestSelectionMatcher
// see https://github.com/gradle/gradle/blob/master/subprojects/testing-base/src/main/java/org/gradle/api/internal/tasks/testing/filter/TestSelectionMatcher.java
final class TestFilter(
  includes: Set[TestFilterPattern],
  excludes: Set[TestFilterPattern],
  commandLineIncludes: Set[TestFilterPattern]
):
  import TestFilterPattern.Match
  import TestFilter.Matches

  def mayIncludeClass(className: String): Boolean = matchesClass(className).nonEmpty

  def matchesClass(className: String): Option[Matches] =
    def matches(patterns: Set[TestFilterPattern]): Option[Matches] =
      if patterns.isEmpty
      then Some(Matches.Suite(explicitlySpecified = false)) else
        val result: Set[Match] = patterns.flatMap(_.matches(className))
        if result.isEmpty then None else
        if result.contains(Match.Suite)
        then Some(Matches.Suite(explicitlySpecified = true))
        else Some(Matches.Tests(
          testNames     = for case Match.TestName    (testName    ) <- result yield testName,
          testWildCards = for case Match.TestWildCard(testWildCard) <- result yield testWildCard
        ))

    // Note: any exclusion of even one method excludes the class
    //      excludes.exists(pattern =>pattern.patternMatches(pattern.determineTargetClassName(className)))
    val excluded: Boolean = excludes.nonEmpty && matches(excludes).isDefined
    if excluded then None else
      for
        included           : Matches <- matches(includes)
        commandLineIncluded: Matches <- matches(commandLineIncludes)
        intersection: Matches = included.intersect(commandLineIncluded)
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
    def toPatterns(strings: Set[String]): Set[TestFilterPattern] = strings.map(TestFilterPattern(_))
    new TestFilter(
      includes = toPatterns(includes),
      excludes = toPatterns(excludes),
      commandLineIncludes = toPatterns(commandLineIncludes)
    )

  enum Matches derives CanEqual:
    case Suite(
      explicitlySpecified: Boolean
    )

    case Tests(
      testNames: Set[String],
      testWildCards: Set[String]
    )

    def isEmpty: Boolean = this match
      case Matches.Tests(testNames, testWildCards) => testNames.isEmpty && testWildCards.isEmpty
      case _ => false

    def intersect(that: Matches): Matches = (this, that) match
      case (Suite(les), Suite(res)) => Suite(les || res)
      case (_: Suite, _) => that
      case (_, _: Suite) => this
      case (l: Tests, r: Tests) => Tests(
        testNames     = l.testNames    .intersect(r.testNames    ) ++
          l.testNames.filter(l => r.testWildCards.exists(l.contains)), // TODO
        testWildCards = l.testWildCards.intersect(r.testWildCards)
      )
