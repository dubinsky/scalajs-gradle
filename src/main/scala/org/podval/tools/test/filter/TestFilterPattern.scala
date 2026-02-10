package org.podval.tools.test.filter

import org.podval.tools.util.Strings
import scala.annotation.tailrec
import java.util.regex.Pattern

// Based on org.gradle.api.internal.tasks.testing.filter.TestSelectionMatcher.TestPattern;
// see https://github.com/gradle/gradle/blob/master/platforms/software/testing-base-infrastructure/src/main/java/org/gradle/api/internal/tasks/testing/filter/TestSelectionMatcher.java#L140
final class TestFilterPattern(pattern: String):
  override def toString: String = pattern

  private def isUpperCase(string: String) = string.nonEmpty && Character.isUpperCase(string.head)
  
  // TODO rework using map/zip
  private val patternPrepared: Pattern =
    val builder: StringBuilder = StringBuilder()
    def appendWildcard(): Unit = builder.append(".*") // replace wildcard '*' with '.*'
    for s: String <- pattern.split("\\*", -1) do
      if s.isEmpty then appendWildcard() else
        if builder.nonEmpty then appendWildcard()
        builder.append(Pattern.quote(s)) //quote everything else
    Pattern.compile(builder.toString)

  private def forMethod: Option[TestFilterPatternMatch] =
    val ss: Seq[String] = pattern.split("\\.", -1).toSeq
    val hasMethod: Boolean =
      ss.nonEmpty && 
      ss.last.nonEmpty && 
      !isUpperCase(ss.last) && 
      (ss.init.exists(isUpperCase) || ss.init.exists(_.contains('*')))
    TestFilterPatternMatch.forMethod(Option.when(hasMethod)(ss.last))
    
  def matchClass(classNameStr: String): Option[TestFilterPatternMatch] =
    val targetClassName: String =
      val withPackage: Boolean = !isUpperCase(pattern)
      if withPackage then classNameStr else Strings
        .split(classNameStr, '.')
        ._2
        .filterNot(_.isEmpty)
        .getOrElse(classNameStr)

    val patternMatches: Boolean = patternPrepared.matcher(targetClassName).matches()
    // TODO [filter] patternPrepared does not take method into account?
    // if !patternMatches then None else
      val segments: Seq[String] = pattern.takeWhile(_ != '*').split("\\.", -1).toSeq
      val patternStartsWithWildcard: Boolean = segments.isEmpty // TODO [filter] pattern.startsWith("*")
      if patternStartsWithWildcard then Some(TestFilterPatternMatch.Suite) else
        val className: Seq[String] = targetClassName.split("\\.").toSeq
        val classNameIsShorterThanPattern: Boolean = className.length < segments.length - 1 // TODO [filter] -1?!
        if classNameIsShorterThanPattern then None else matches(segments, className)

  @tailrec
  private def matches(
    segments: Seq[String],
    className: Seq[String]
  ): Option[TestFilterPatternMatch] = if segments.isEmpty then None else
    val patternHasWildcards: Boolean = pattern.contains('*')
    val classElement: String = className.head
    val patternElement: String = segments.head
    // Foo can match both Foo and Foo$NestedClass (https://github.com/gradle/gradle/issues/5763)
    val classMatches: Boolean = classElement == patternElement.takeWhile(_ != '$')

    val lastClassNameElementMatchesPenultimatePatternElement: Boolean = 
      className.length == 1 && segments.length == 2 && classMatches
    
    val lastClassNameElementMatchesLastPatternElement: Boolean =
      // TODO [filter] why does this condition break things if we are dealing with the last class name element?
      //  className.length == 1 &&
      segments.length == 1 && (classMatches || (patternHasWildcards && classElement.startsWith(patternElement)))

    if lastClassNameElementMatchesPenultimatePatternElement then forMethod else
      if lastClassNameElementMatchesLastPatternElement then forMethod else
        if classElement != patternElement then None else matches(segments.tail, className.tail)
