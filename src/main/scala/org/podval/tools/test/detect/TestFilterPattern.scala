package org.podval.tools.test.detect

// TODO turn into a sealed trait
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

  private def determineTargetClassName(className: String): String =
    if !patternStartsWithUpperCase then className else // with package
      val simpleName: String = StringUtils.substringAfterLast(className, ".") // without package
      if simpleName.nonEmpty then simpleName else className
  
  def matches(classNameStr: String): Option[TestFilterPatternMatch] =
    if patternStartsWithWildcard then Some(TestFilterPatternMatch.Suite) else
      val className: List[String] = determineTargetClassName(classNameStr).split("\\.").toList
      val classNameIsShorterThanPattern: Boolean = className.length < segments.length - 1 // TODO [filter] -1?!
      if classNameIsShorterThanPattern then None else
        TestFilterPatternMatch.matches(
          segments,
          className,
          methodSuffix
        )
