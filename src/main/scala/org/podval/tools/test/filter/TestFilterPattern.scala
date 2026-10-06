package org.podval.tools.test.filter

import org.podval.tools.util.Strings
import java.util.regex.Pattern

// Class-name half of org.gradle.api.internal.tasks.testing.filter.ClassTestSelectionMatcher
// (Gradle 9.8, ClassTestPattern). Method selectors are this plugin's addition:
// the sbt test interface wants a method name or a prefix, which Gradle does not.
final class TestFilterPattern(pattern: String):
  override def toString: String = pattern

  private val star: Int = pattern.indexOf('*')
  private val bracket: Int = pattern.indexOf('[')

  private val segments: Seq[String] =
    if star >= 0 then TestFilterPattern.splitPreserveAllTokens(pattern.substring(0, star), '.')
    else if bracket < 0 then TestFilterPattern.splitPreserveAllTokens(pattern, '.')
    else
      val cut: Seq[String] = TestFilterPattern.splitPreserveAllTokens(pattern.substring(0, bracket), '.')
      if cut.isEmpty then Seq(pattern.substring(bracket))
      else cut.init :+ (cut.last + pattern.substring(bracket))

  private val simpleName: Boolean = pattern.nonEmpty && Character.isUpperCase(pattern.head)

  private def targetClassName(className: String): String =
    if !simpleName then className
    else Strings.split(className, '.')._2.filterNot(_.isEmpty).getOrElse(className)

  def matchClass(className: String): Option[TestFilterPatternMatch] =
    if !mayIncludeClass(className) then None
    else TestFilterPatternMatch.forMethod(methodSegment)

  private def mayIncludeClass(className: String): Boolean =
    if segments.isEmpty then true else
      // Java String.split drops trailing empty segments, matching Gradle's ClassTestPattern.
      val classSegments: Seq[String] = targetClassName(className).split("\\.").toSeq
      if classSegments.length < segments.length - 1 then false
      else matches(segments, classSegments)

  private def matches(patternSegments: Seq[String], classSegments: Seq[String]): Boolean =
    patternSegments match
      case Seq() => false
      case patternHead +: patternTail =>
        val classHead: String = classSegments.head
        val atLastClass: Boolean = classSegments.length == 1
        val penultimate: Boolean =
          patternTail.length == 1 && atLastClass && TestFilterPattern.classNameMatch(classHead, patternHead)
        val last: Boolean =
          patternTail.isEmpty && TestFilterPattern.lastElementMatch(classHead, patternHead, star >= 0)
        if penultimate || last then true
        else if classHead != patternHead then false
        else matches(patternTail, classSegments.tail)

  // The final dotted piece, when it is a method rather than a class.
  private def methodSegment: Option[String] =
    val pieces: Seq[String] = pattern.split("\\.", -1).toSeq
    val hasMethod: Boolean =
      pieces.nonEmpty &&
      pieces.last.nonEmpty &&
      !TestFilterPattern.isUpperCase(pieces.last) &&
      (pieces.init.exists(TestFilterPattern.isUpperCase) || pieces.init.exists(_.contains('*')))
    Option.when(hasMethod)(pieces.last)

object TestFilterPattern:
  private def isUpperCase(string: String): Boolean = string.nonEmpty && Character.isUpperCase(string.head)

  // Apache StringUtils.splitPreserveAllTokens: an empty input is an empty array.
  // Java's String.split turns "" into Array("").
  private def splitPreserveAllTokens(text: String, separator: Char): Seq[String] =
    if text.isEmpty then Seq.empty
    else text.split(Pattern.quote(separator.toString), -1).toSeq

  // A pattern element that contains '$' also matches the name before that '$'.
  // Enclosing$Nested matches Enclosing. Enclosing does not match Enclosing$Nested.
  private def classNameMatch(classElement: String, patternElement: String): Boolean =
    if classElement == patternElement then true
    else if patternElement.contains("$") then
      classElement == patternElement.takeWhile(_ != '$')
    else false

  private def lastElementMatch(classElement: String, patternElement: String, wildcard: Boolean): Boolean =
    val exact: Boolean = classNameMatch(classElement, patternElement)
    exact || (wildcard && classElement.startsWith(patternElement))
