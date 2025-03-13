package org.podval.tools.test.filter

final class TestFilterPatterns(patterns: Set[TestFilterPattern]):
  def isEmpty: Boolean = patterns.isEmpty

  def matchClass(className: String): Option[TestFilterMatch] =
    if patterns.isEmpty then Some(SuiteTestFilterMatch(explicitlySpecified = false)) else
      val result: Set[TestFilterPatternMatch] = patterns.flatMap(_.matchClass(className))
      if result.isEmpty then None else
        if result.contains(TestFilterPatternMatch.Suite)
        then Some(SuiteTestFilterMatch(explicitlySpecified = true))
        else Some(TestsTestFilterMatch(
          testNames     = for case TestFilterPatternMatch.TestName    (testName    ) <- result yield testName,
          testWildCards = for case TestFilterPatternMatch.TestWildCard(testWildCard) <- result yield testWildCard
        ))
