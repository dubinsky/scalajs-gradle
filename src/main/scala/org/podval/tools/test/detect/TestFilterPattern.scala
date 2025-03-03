package org.podval.tools.test.detect

import scala.annotation.tailrec

// TODO [filter] turn into a sealed trait
final class TestFilterPattern(pattern: String):
  override def toString: String = pattern

  private val (beforeWildcard: String, methodSuffix: String) =
    val firstWildcardIndex: Int = pattern.indexOf('*')
    if firstWildcardIndex == -1 then (pattern, "") else
      val result: String = pattern.substring(firstWildcardIndex + 1)
      (
        pattern.substring(0, firstWildcardIndex),
        if result.startsWith(".") then result.tail else "*" + result // TODO [filter] move down
      )

  private val segments: List[String] = StringUtils.splitPreserveAllTokens(beforeWildcard, '.').toList
  private val patternStartsWithWildcard: Boolean = segments.isEmpty
  private val patternStartsWithUpperCase: Boolean = pattern.nonEmpty && Character.isUpperCase(pattern.head)

  def matchClass(classNameStr: String): Option[TestFilterPatternMatch] =
    if patternStartsWithWildcard then Some(TestFilterPatternMatch.Suite) else
      val className: List[String] = determineTargetClassName(classNameStr).split("\\.").toList
      val classNameIsShorterThanPattern: Boolean = className.length < segments.length - 1 // TODO [filter] -1?!
      if classNameIsShorterThanPattern then None else
        TestFilterPattern.matches(
          segments,
          className,
          methodSuffix
        )

  private def determineTargetClassName(className: String): String =
    if !patternStartsWithUpperCase then className else // with package
      val simpleName: String = StringUtils.substringAfterLast(className, ".") // without package
      if simpleName.nonEmpty then simpleName else className

object TestFilterPattern:
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

    if lastClassNameElementMatchesPenultimatePatternElement
    then TestFilterPatternMatch.forMethod(segments.last + methodSuffix) else
      val lastClassNameElementMatchesLastPatternElement: Boolean =
        (segments.length == 1) &&
        (
          (classElement == patternElementOuter) ||
          (methodSuffix.nonEmpty && classElement.startsWith(patternElement)) // TODO [filter]
        )

      if lastClassNameElementMatchesLastPatternElement
      then TestFilterPatternMatch.forMethod(methodSuffix) else
        if classElement != patternElement
        then None
        else matches(
          segments.tail,
          className.tail,
          methodSuffix
        )
