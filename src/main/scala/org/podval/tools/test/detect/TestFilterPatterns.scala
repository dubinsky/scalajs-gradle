package org.podval.tools.test.detect

final class TestFilterPatterns(patterns: Set[TestFilterPattern]):
  def isEmpty: Boolean = patterns.isEmpty

  def matchClass(className: String): Option[TestFilterMatch] =
    if patterns.isEmpty then Some(TestFilterMatch.Suite(explicitlySpecified = false)) else
      val result: Set[TestFilterPatternMatch] = patterns.flatMap(_.matchClass(className))
      if result.isEmpty then None else
        if result.contains(TestFilterPatternMatch.Suite)
        then Some(TestFilterMatch.Suite(explicitlySpecified = true))
        else Some(TestFilterMatch.Tests(
          testNames     = for case TestFilterPatternMatch.TestName    (testName    ) <- result yield testName,
          testWildCards = for case TestFilterPatternMatch.TestWildCard(testWildCard) <- result yield testWildCard
        ))
