package org.podval.tools.test.taskdef

import org.gradle.api.internal.tasks.testing.TestDefinition
import org.gradle.api.internal.tasks.testing.filter.TestSelectionMatcher
import org.podval.tools.test.framework.Framework
import org.podval.tools.platform.Scala212Collections.arrayMkString
import sbt.testing.{AnnotatedFingerprint, Fingerprint, SubclassFingerprint}

final class TestClassRun(
  val framework: Framework.Loaded,
  val className: String,
  val fingerprint: Fingerprint,
  val explicitlySpecified: Boolean,
  val testNames: Array[String],
  val testWildcards: Array[String]
) extends TestDefinition:
  override def getId: String = className
  override def getDisplayName: String = s"test class $className'"
  override def matches(matcher: TestSelectionMatcher): Boolean = matcher.mayIncludeClass(getId)

object TestClassRun:
  // I do not want to go into quoting,
  // nor do I want to make the result unreadable by using string length,
  // so I just assume that the separators do not occur in test names
  // and won't be supplied maliciously :)
  // Also, separator is a regex, so if the character I use is weird enough,
  // it probably means something to the regex processor :(
  private val separator: String = "---"
  private val stringsSeparator: String = "-,-"

  def write(value: TestClassRun): String = arrayMkString(toStrings(value), separator)
  private def toStrings(value: TestClassRun): Array[String] =
    // array of length 4
    val fingerprintStrings = value.fingerprint match
      case annotated: AnnotatedFingerprint => Array(
        toString(true),
        annotated.annotationName,
        toString(annotated.isModule),
        "<dummy>"
      )
      case subclass: SubclassFingerprint => Array(
        toString(false),
        subclass.superclassName,
        toString(subclass.isModule),
        toString(subclass.requireNoArgConstructor)
      )

    Array(
      value.framework.name,
      value.className,
      fingerprintStrings(0),
      fingerprintStrings(1),
      fingerprintStrings(2),
      fingerprintStrings(3),
      toString(value.explicitlySpecified),
      writeStrings(value.testNames),
      writeStrings(value.testWildcards)
    )

  // Thank you, sjrd, for the split trivia!
  // (https://github.com/scala-js/scala-js/pull/5132#discussion_r1967584316)
  final def read(string: String): TestClassRun = fromStrings(string.split(separator, -1))
  private def fromStrings(strings: Array[String]): TestClassRun = TestClassRun(
    framework = Framework.forName(strings(0)).load,
    className = strings(1),
    fingerprint =
      val isAnnotated: Boolean = toBoolean(strings(2))
      val name: String = strings(3)
      val isModule: Boolean = toBoolean(strings(4))
      if isAnnotated
      then AnnotatedFingerprintImpl(
        annotationName = name,
        isModule = isModule
      )
      else SubclassFingerprintImpl(
        superclassName = name,
        isModule = isModule,
        requireNoArgConstructor = toBoolean(strings(5))
      ),
    explicitlySpecified = toBoolean(strings(6)),
    testNames = readStrings(strings(7)),
    testWildcards = readStrings(strings(8))
  )

  private def toString(boolean: Boolean): String = boolean.toString
  private def toBoolean(string: String): Boolean = string == "true"

  private def writeStrings(strings: Array[String]): String = arrayMkString(strings, stringsSeparator)
  private def readStrings(string: String): Array[String] =
    // Now that we split with "-1",
    // empty string turns into an array with one empty string in it...
    val strings: Array[String] = string.split(stringsSeparator)
    if strings.length == 1 && strings(0).isEmpty
    then Array.empty
    else strings
