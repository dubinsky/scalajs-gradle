package org.podval.tools.test

import org.podval.tools.test.TestFiltering.TestPattern
import sbt.testing.{Selector, SuiteSelector, TestSelector, TestWildcardSelector}
import java.util.regex.Pattern
import scala.annotation.tailrec

  // Note: since there is no way to find out what tests are defined in a given suite,
  // I need to know the meaning of the inclusion patterns (and can't handle individual test exclusions).

  // Note: heavily based on org.gradle.api.internal.tasks.testing.filter.TestSelectionMatcher
  //   see https://github.com/gradle/gradle/blob/master/subprojects/testing-base/src/main/java/org/gradle/api/internal/tasks/testing/filter/TestSelectionMatcher.java

  /**
   * This class has two public APIs:
   *
   * <ul>
   * <li>Judge whether a test class might be included. For example, class 'org.gradle.Test' can't
   * be included by pattern 'org.apache.Test'
   * <li>Judge whether a test method is matched exactly.
   * </ul>
   *
   * In both cases, if the pattern starts with an upper-case letter, it will be used to match
   * simple class name;
   * otherwise, it will be used to match full qualified class name.
   */
final class TestFiltering(
  buildScriptIncludePatterns: Set[TestPattern],
  buildScriptExcludePatterns: Set[TestPattern],
  commandLineIncludePatterns: Set[TestPattern]
):
  def whatToIncludeForClass(className: String): (Boolean, Array[Selector]) =
    if !mayIncludeClass(className)
    then (false, Array.empty)
    else (false,
      Array(new SuiteSelector)
//        Seq(new TestSelector("2*2 success should work"))
//      Seq(new TestWildcardSelector("2*2"))
    )

  def matchesTest(className: String, methodName: String): Boolean =
    def matchesClassAndMethod(patterns: Set[TestPattern]): Boolean = patterns
      .exists(pattern => pattern.matchesClassAndMethod(className, methodName) || pattern.matchesClass(className))

    def matchesPattern(includePatterns: Set[TestPattern]) : Boolean =
      includePatterns.isEmpty || matchesClassAndMethod(includePatterns)

    // When there is a class name match, return true for excluding it so that we can keep
    // searching in individual test methods for an exact match. If we return false here
    // instead, then we'll give up searching individual test methods and just ignore the
    // entire test class, which may not be what we want.
    def matchesExcludePattern: Boolean = buildScriptExcludePatterns.nonEmpty && (
      (TestFiltering.mayMatchClass(buildScriptExcludePatterns, className) && (methodName == null)) ||
      matchesClassAndMethod(buildScriptExcludePatterns)
    )

    matchesPattern(buildScriptIncludePatterns) &&
    matchesPattern(commandLineIncludePatterns) &&
    !matchesExcludePattern

  def mayIncludeClass(fullQualifiedName: String): Boolean =
    def mayIncludeClass(includePatterns: Set[TestPattern]): Boolean =
      includePatterns.isEmpty || TestFiltering.mayMatchClass(includePatterns, fullQualifiedName)

    mayIncludeClass(buildScriptIncludePatterns) &&
    mayIncludeClass(commandLineIncludePatterns) &&
    !buildScriptExcludePatterns.exists(_.matchesClass(fullQualifiedName))

object TestFiltering:

  def apply(
    includes: Set[String],
    excludes: Set[String],
    commandLineIncludes: Set[String]
  ): TestFiltering = new TestFiltering(
    includes.map(TestPattern(_)),
    excludes.map(TestPattern(_)),
    commandLineIncludes.map(TestPattern(_))
  )

  private def mayMatchClass(patterns: Set[TestPattern], fullQualifiedName: String): Boolean =
    patterns.exists(_.mayIncludeClass(fullQualifiedName))

  final class TestPattern(patternIn: String):
    private val pattern: Pattern = preparePattern(patternIn)

    private val classNameSelector: ClassNameSelector =
      if patternStartsWithUpperCase(patternIn)
      then new SimpleClassNameSelector()
      else new FullQualifiedClassNameSelector()

    private val firstWildcardIndex: Int = patternIn.indexOf('*')

    private val segments: Array[String] =
      splitPreserveAllTokens(
        if firstWildcardIndex == -1 then patternIn else patternIn.substring(0, firstWildcardIndex),
        '.'
      )

    private val lastElementMatcher: LastElementMatcher =
      if firstWildcardIndex == -1
      then new NoWildcardMatcher()
      else new WildcardMatcher()

    def mayIncludeClass(fullQualifiedName: String): Boolean =
      if patternStartsWithWildcard then
        true
      else
        val classNameArray: Array[String] =
          classNameSelector.determineTargetClassName(fullQualifiedName).split("\\.")

        if classNameIsShorterThanPattern(classNameArray) then false else
          // TODO not sure what's happening here, so translating literally for now
          val results: List[Option[Boolean]] = for (i: Int <- segments.indices.toList) yield
            if lastClassNameElementMatchesPenultimatePatternElement(classNameArray, i) then Some(true)
            else if lastClassNameElementMatchesLastPatternElement(classNameArray, i) then Some(true)
            else if classNameArray(i) != segments(i) then Some(false)
            else None
          results.flatten.headOption.getOrElse(false)

    def matchesClass(fullQualifiedName: String): Boolean =
      pattern.matcher(classNameSelector.determineTargetClassName(fullQualifiedName)).matches

    def matchesClassAndMethod(fullQualifiedName: String, methodName: String): Boolean =
      (methodName != null) &&
      pattern.matcher(classNameSelector.determineTargetClassName(fullQualifiedName) + "." + methodName).matches

    private def lastClassNameElementMatchesPenultimatePatternElement(className: Array[String], index: Int): Boolean =
      (index == segments.length - 2) &&
      (index == className.length - 1) &&
      classNameMatch(className(index), segments(index))

    private def lastClassNameElementMatchesLastPatternElement(className: Array[String], index: Int): Boolean =
      (index == segments.length - 1) &&
      lastElementMatcher.matches(className(index), segments(index))

    private def patternStartsWithWildcard: Boolean =
      segments.length == 0

    private def classNameIsShorterThanPattern(classNameArray: Array[String]): Boolean =
      classNameArray.length < segments.length - 1

  private def patternStartsWithUpperCase(pattern: String): Boolean =
    pattern.nonEmpty && Character.isUpperCase(pattern.charAt(0))

  private def preparePattern(input: String): Pattern =
    val pattern: java.lang.StringBuilder = new java.lang.StringBuilder
    for s: String <- splitPreserveAllTokens(input, '*') do
      if s == "" then pattern.append(".*") else //replace wildcard '*' with '.*'
        if pattern.length > 0 then pattern.append(".*") //replace wildcard '*' with '.*'
        pattern.append(Pattern.quote(s)); //quote everything else

    Pattern.compile(pattern.toString)

  private sealed trait LastElementMatcher:
    def matches(classElement: String, patternElement: String): Boolean

  private final class NoWildcardMatcher extends LastElementMatcher:
    override def matches(classElement: String, patternElement: String): Boolean =
      classNameMatch(classElement, patternElement)

  private final class WildcardMatcher extends LastElementMatcher:
    override def matches(classElement: String, patternElement: String): Boolean =
      classElement.startsWith(patternElement) ||
      classNameMatch(classElement, patternElement)

  // Foo can match both Foo and Foo$NestedClass
  // https://github.com/gradle/gradle/issues/5763
  private def classNameMatch(simpleClassName: String, patternSimpleClassName: String): Boolean =
    (simpleClassName == patternSimpleClassName) ||
    (
      patternSimpleClassName.contains("$") &&
      (simpleClassName == patternSimpleClassName.substring(0, patternSimpleClassName.indexOf('$')))
    )

  private sealed trait ClassNameSelector:
    def determineTargetClassName(fullQualifiedName: String): String

  private final class FullQualifiedClassNameSelector extends ClassNameSelector:
    override def determineTargetClassName(fullQualifiedName: String): String =
      fullQualifiedName

  private final class SimpleClassNameSelector extends ClassNameSelector:
    override def determineTargetClassName(fullQualifiedName: String): String =
      getSimpleName(fullQualifiedName)

  private def getSimpleName(fullQualifiedName: String): String =
    val simpleName: String = substringAfterLast(fullQualifiedName, ".")
    if simpleName != "" then simpleName else fullQualifiedName

  /* Note: based on org.apache.commons.lang3.StringUtils

  Gets the substring after the last occurrence of a separator.
  A null string input will return null.
  An empty ("") string input will return the empty string.
  An empty or null separator will return the empty string if the input string is not null.
  If nothing is found, the empty string is returned.
  */
  def substringAfterLast(string: String, separator: String): String =
    if string == null then null else
    if string.isEmpty then "" else
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

    if string == null then null else
    if string.isEmpty then Array.empty
    else split(Seq.empty, string).toArray


  def main(args: Array[String]): Unit =
    run("org.gradle.FooTest.testMethod", "org.gradle.FooTest")

  private def run(input: String, className: String): Unit =
    TestFiltering(Set(input), Set.empty, Set.empty)
      .mayIncludeClass(className)
