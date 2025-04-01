package org.podval.tools.test.taskdef

import org.gradle.api.internal.tasks.testing.TestClassRunInfo
import org.podval.tools.util.Scala212Collections.arrayMkString
import sbt.testing.{AnnotatedFingerprint, Fingerprint, SubclassFingerprint}

final class TestClassRun(
  val frameworkProvider: FrameworkProvider,
  override val getTestClassName: String,
  val fingerprint: Fingerprint,
  val explicitlySpecified: Boolean,
  val testNames: Array[String],
  val testWildCards: Array[String]
) extends TestClassRunInfo

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
      value.frameworkProvider.frameworkName,
      value.getTestClassName,
      fingerprintStrings(0),
      fingerprintStrings(1),
      fingerprintStrings(2),
      fingerprintStrings(3),
      toString(value.explicitlySpecified),
      writeStrings(value.testNames),
      writeStrings(value.testWildCards)
    )

  // Thank you, sjrd, for the split trivia!
  // (https://github.com/scala-js/scala-js/pull/5132#discussion_r1967584316)
  final def read(string: String): TestClassRun = fromStrings(string.split(separator, -1))
  private def fromStrings(strings: Array[String]): TestClassRun = TestClassRun(
    frameworkProvider = FrameworkProviderForking(frameworkName = strings(0)),
    getTestClassName = strings(1),
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
    testWildCards = readStrings(strings(8))
  )

  private def writeStrings(strings: Array[String]): String = arrayMkString(strings, stringsSeparator)
  private def readStrings(string: String): Array[String] =
    // Now that we split with "-1",
    // empty string turns into an array with one empty string in it...
    val strings: Array[String] = string.split(stringsSeparator)
    if strings.length == 1 && strings(0).isEmpty
    then Array.empty
    else strings

  private def toString(boolean: Boolean): String = boolean.toString
  private def toBoolean(string: String): Boolean = string == "true"
