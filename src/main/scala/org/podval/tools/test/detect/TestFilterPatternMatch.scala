package org.podval.tools.test.detect

import scala.annotation.tailrec

enum TestFilterPatternMatch:
  case Suite
  case TestName(testName: String)
  case TestWildCard(testWildCard: String)
  
object TestFilterPatternMatch:
  @tailrec
  def matches(
    segments: List[String],
    className: List[String],
    methodSuffix: String
  ): Option[TestFilterPatternMatch] = if segments.isEmpty then None else
    val classElement: String = className.head
    val patternElement: String = segments.head

    // TODO [filter] detect nested suites/tests
    // Foo can match both Foo and Foo$NestedClass
    // https://github.com/gradle/gradle/issues/5763
    val patternElementOuter: String =
      val dollarIndex: Int = patternElement.indexOf('$')
      if dollarIndex == -1
      then patternElement
      else patternElement.substring(0, dollarIndex)

    val lastClassNameElementMatchesPenultimatePatternElement: Boolean =
      (segments.length == 2) &&
      (className.length == 1) &&
      (classElement == patternElementOuter)

    if lastClassNameElementMatchesPenultimatePatternElement then forMethod(segments.last + methodSuffix) else
      val lastClassNameElementMatchesLastPatternElement: Boolean =
        (segments.length == 1) &&
        (
          (classElement == patternElementOuter) ||
          (methodSuffix.nonEmpty && classElement.startsWith(patternElement)) // TODO [filter]
        )

      if lastClassNameElementMatchesLastPatternElement then forMethod(methodSuffix) else
        if classElement != patternElement
        then None
        else matches(
          segments.tail,
          className.tail,
          methodSuffix
        )
 
  private def forMethod(method: String): Option[TestFilterPatternMatch] =
    if method.isEmpty || method == "*" then Some(Suite) else
    if method.head == '*' && method.last == '*' then forWildCard(method.tail.init) else
    if method.head == '*' then forWildCard(method.tail) else
    if method.last == '*' then forWildCard(method.init) else
      Some(TestName(method))
      
  private def forWildCard(wildCard: String): Option[TestFilterPatternMatch] =
    if wildCard.contains("*")
    then Some(Suite) // wildCard is not expressible using Selectors
    else Some(TestWildCard(wildCard))
