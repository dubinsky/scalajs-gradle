package org.podval.tools.test

//import java.util.regex.Pattern
import scala.annotation.tailrec

final class TestFilterPattern(pattern: String):
  override def toString: String = pattern

  def determineTargetClassName(className: String): String =
    val patternStartsWithUpperCase: Boolean = pattern.nonEmpty && Character.isUpperCase(pattern.head)
    if !patternStartsWithUpperCase then className else // with package
      val simpleName: String = TestFilterPattern.substringAfterLast(className, ".") // without package
      if simpleName.nonEmpty then simpleName else className

//  private val patternCompiled: Pattern =
//    val patternBuilder: java.lang.StringBuilder = new java.lang.StringBuilder
//    for s: String <- TestFilterPattern.splitPreserveAllTokens(pattern, '*') do
//      if s == "" then patternBuilder.append(".*") else //replace wildcard '*' with '.*'
//        if patternBuilder.length > 0 then patternBuilder.append(".*") //replace wildcard '*' with '.*'
//        patternBuilder.append(Pattern.quote(s)); //quote everything else
//
//    Pattern.compile(patternBuilder.toString)
//
//  def patternMatches(name: String): Boolean = patternCompiled.matcher(name).matches

  def matches(classNameStr: String): Option[TestFilterPattern.Match] =
    val firstWildcardIndex: Int = pattern.indexOf('*')

    val segments: List[String] = TestFilterPattern.splitPreserveAllTokens(
      if firstWildcardIndex == -1 then pattern else pattern.substring(0, firstWildcardIndex),
      '.'
    ).toList

    val patternStartsWithWildcard: Boolean = segments.isEmpty
    if patternStartsWithWildcard then Some(TestFilterPattern.Match.Suite) else
      val className: List[String] = determineTargetClassName(classNameStr).split("\\.").toList
      val classNameIsShorterThanPattern: Boolean = className.length < segments.length - 1 // TODO -1?!
      if classNameIsShorterThanPattern then None else
        val methodSuffix: String = if firstWildcardIndex == -1 then "" else
          val result: String = pattern.substring(firstWildcardIndex + 1)
          if result.startsWith(".") then result.tail else "*" + result // TODO move down

        TestFilterPattern.matches(
          segments,
          className,
          methodSuffix
        )

object TestFilterPattern:
  enum Match:
    case Suite
    case TestName(testName: String)
    case TestWildCard(testWildCard: String)

  @tailrec
  private def matches(
    segments: List[String],
    className: List[String],
    methodSuffix: String
  ): Option[Match] = if segments.isEmpty then None else
    val classElement: String = className.head
    val patternElement: String = segments.head

    // TODO detect nested suites/tests
    // Foo can match both Foo and Foo$NestedClass
    // https://github.com/gradle/gradle/issues/5763
    val patternElementOuter =
      val dollarIndex: Int = patternElement.indexOf('$')
      if dollarIndex == -1
      then patternElement
      else patternElement.substring(0, dollarIndex)

    val lastClassNameElementMatchesPenultimatePatternElement: Boolean =
      (segments.length == 2) &&
      (className.length == 1) &&
      (classElement == patternElementOuter)

    if lastClassNameElementMatchesPenultimatePatternElement
    then forMethod(segments.last + methodSuffix) else
      val lastClassNameElementMatchesLastPatternElement: Boolean =
        (segments.length == 1) &&
        (
          (classElement == patternElementOuter) ||
          (methodSuffix.nonEmpty && classElement.startsWith(patternElement)) // TODO
        )

      if lastClassNameElementMatchesLastPatternElement
      then forMethod(methodSuffix) else
        if classElement != patternElement
        then None
        else matches(
          segments.tail,
          className.tail,
          methodSuffix
        )

  private def forMethod(method: String): Option[Match] =
    if method.isEmpty || method == "*" then Some(Match.Suite) else
      def forWildCard(wildCard: String): Option[Match] =
        if wildCard.contains("*")
        then Some(Match.Suite) // wildCard is not expressible using Selectors
        else Some(Match.TestWildCard(wildCard))

      if method.head == '*' && method.last == '*' then forWildCard(method.tail.init) else
      if method.head == '*' then forWildCard(method.tail) else
      if method.last == '*' then forWildCard(method.init) else
        Some(Match.TestName(method))


  /* Note: based on org.apache.commons.lang3.StringUtils

  Gets the substring after the last occurrence of a separator.
  A null string input will return null.
  An empty ("") string input will return the empty string.
  An empty or null separator will return the empty string if the input string is not null.
  If nothing is found, the empty string is returned.
  */
  def substringAfterLast(string: String, separator: String): String =
    if string == null then null else if string.isEmpty then "" else
      val index: Int = string.lastIndexOf(separator)
      if index == -1 then ""
      else string.substring(index + separator.length)

  /* Note: based on org.apache.commons.lang3.StringUtils

  Splits the provided text into an array, separator specified, preserving all tokens,
  including empty tokens created by adjacent separators.

  A null input String returns null. null splits on whitespace
  */
  def splitPreserveAllTokens(string: String, separator: Char): Array[String] =
    @tailrec def split(acc: Seq[String], string: String): Seq[String] =
      val (token: String, tail: String) = string.span(_ != separator)
      val newAcc: Seq[String] = acc :+ token
      if tail.isEmpty then newAcc else split(newAcc, tail.drop(1))

    if string == null then null else if string.isEmpty then Array.empty
    else split(Seq.empty, string).toArray
