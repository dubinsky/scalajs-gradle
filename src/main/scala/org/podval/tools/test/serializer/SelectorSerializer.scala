package org.podval.tools.test.serializer

import org.gradle.internal.serialize.{Decoder, Encoder, Serializer}
import sbt.testing.{NestedSuiteSelector, NestedTestSelector, Selector, SuiteSelector, TestSelector, TestWildcardSelector}

class SelectorSerializer extends Serializer[Selector]:
  import SelectorSerializer.*

  override def write(encoder: Encoder, value: Selector): Unit = value match
    case suiteSelector: SuiteSelector =>
      encoder.writeByte(Suite)
    case testSelector: TestSelector =>
      encoder.writeByte(Test)
      encoder.writeString(testSelector.testName)
    case nestedSuiteSelector: NestedSuiteSelector =>
      encoder.writeByte(NestedSuite)
      encoder.writeString(nestedSuiteSelector.suiteId)
    case nestedTestSelector: NestedTestSelector =>
      encoder.writeByte(NestedTest)
      encoder.writeString(nestedTestSelector.suiteId)
      encoder.writeString(nestedTestSelector.testName)
    case testWildcardSelector: TestWildcardSelector =>
      encoder.writeByte(TestWildcard)
      encoder.writeString(testWildcardSelector.testWildcard)

  override def read(decoder: Decoder): Selector = decoder.readByte match
    case Suite =>
      new SuiteSelector
    case Test =>
      val testName: String = decoder.readString
      TestSelector(testName)
    case NestedSuite =>
      val suiteId: String = decoder.readString
      NestedSuiteSelector(suiteId)
    case NestedTest =>
      val suiteId: String = decoder.readString
      val testName: String = decoder.readString
      NestedTestSelector(
        suiteId,
        testName
      )
    case TestWildcard =>
      val testWildcard: String = decoder.readString
      TestWildcardSelector(testWildcard)

object SelectorSerializer:
  private val Suite       : Byte = 1
  private val Test        : Byte = 2
  private val NestedSuite : Byte = 3
  private val NestedTest  : Byte = 4
  private val TestWildcard: Byte = 5

  def equal(left: Selector, right: Selector): Boolean = (left, right) match
    case (left: SuiteSelector, right: SuiteSelector) =>
      true
    case (left: TestSelector, right: TestSelector) =>
      left.testName == right.testName
    case (left: NestedSuiteSelector, right: NestedSuiteSelector) =>
      left.suiteId == right.suiteId
    case (left: NestedTestSelector, right: NestedTestSelector) =>
      left.suiteId == right.suiteId &&
      left.testName == right.testName
    case (left: TestWildcardSelector, right: TestWildcardSelector) =>
      left.testWildcard == right.testWildcard
    case _ => false
