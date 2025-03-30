package org.podval.tools.test.filter

final class TestFilterPatterns(patterns: Set[TestFilterPattern]):
  def isEmpty: Boolean = patterns.isEmpty

  def matchClass(className: String): Option[TestFilterMatch] =
    if patterns.isEmpty then Some(SuiteTestFilterMatch(explicitlySpecified = false)) else
      val result: Set[TestFilterPatternMatch] = patterns.flatMap(_.matchClass(className))
      if result.isEmpty then None else
        if result.contains(TestFilterPatternMatch.Suite)
        // Even when a class is singled out by name,
        // `explicitlySpecified` should be `false`;
        // see https://github.com/dubinsky/scalajs-gradle/issues/32
        then Some(SuiteTestFilterMatch(explicitlySpecified = false))
        else Some(TestsTestFilterMatch(
          testNames     = for case TestFilterPatternMatch.TestName    (testName    ) <- result yield testName,
          testWildCards = for case TestFilterPatternMatch.TestWildCard(testWildCard) <- result yield testWildCard
        ))
