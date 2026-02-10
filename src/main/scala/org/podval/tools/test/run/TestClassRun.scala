package org.podval.tools.test.run

import org.gradle.api.internal.tasks.testing.TestDefinition
import org.gradle.api.internal.tasks.testing.filter.TestSelectionMatcher
import org.podval.tools.build.TestFramework
import org.podval.tools.util.Scala212Collections.{arrayConcat, writeStrings, readStrings}
import sbt.testing.{AnnotatedFingerprint, Fingerprint, SubclassFingerprint}

final class TestClassRun(
  val framework: TestFramework.Loaded,
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

  private def writeBoolean(boolean: Boolean): String = boolean.toString
  private def readBoolean(string: String): Boolean = string == "true"

  def write(value: TestClassRun): String = writeStrings(separator = separator, array = arrayConcat(
    Array(
      value.framework.nameSbt,                             /* 0 */
      value.className,                                     /* 1 */
      writeBoolean(value.explicitlySpecified),             /* 2 */
      writeStrings(value.testNames, stringsSeparator),    /* 3 */
      writeStrings(value.testWildcards, stringsSeparator) /* 4 */
    ),
    // array of length 4
    value.fingerprint match
      case annotated: AnnotatedFingerprint => Array(
        writeBoolean(true),                                /* 5 */
        annotated.annotationName,                          /* 6 */
        writeBoolean(annotated.isModule),                  /* 7 */
        "<dummy>"                                          /* 8 */
      )
      case subclass: SubclassFingerprint => Array(
        writeBoolean(false),                               /* 5 */
        subclass.superclassName,                           /* 6 */
        writeBoolean(subclass.isModule),                   /* 7 */
        writeBoolean(subclass.requireNoArgConstructor)     /* 8 */
      )
  ))

  // Thank you, sjrd, for the split trivia!
  // (https://github.com/scala-js/scala-js/pull/5132#discussion_r1967584316)
  final def read(string: String): TestClassRun =
    val strings: Array[String] = string.split(separator, -1)
    TestClassRun(
      framework = TestFramework.forNameSbt(strings(0)).load,
      className = strings(1),
      explicitlySpecified = readBoolean(strings(2)),
      testNames = readStrings(strings(3), stringsSeparator),
      testWildcards = readStrings(strings(4), stringsSeparator),
      fingerprint =
        if readBoolean(strings(5))
        then new AnnotatedFingerprint:
          override val annotationName: String = strings(6)
          override val isModule: Boolean = readBoolean(strings(7))
          override def toString: String = s"AnnotatedFingerprint($annotationName, isModule=$isModule)"
        else new SubclassFingerprint:
          override val superclassName: String = strings(6)
          override val isModule: Boolean = readBoolean(strings(7))
          override val requireNoArgConstructor: Boolean = readBoolean(strings(8))
          override def toString: String = s"SubclassFingerprint($superclassName, isModule=$isModule, requireNoArgConstructor=$requireNoArgConstructor)"
    )

