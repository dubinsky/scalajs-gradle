package org.podval.tools.test.detect

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3}

class StringUtilsTest extends AnyFlatSpec, TableDrivenPropertyChecks, Matchers:
  "splitPreserveAllTokens" should "work" in:
    val data: TableFor3[String, Char, Array[String]] = Table(
      ("string", "separator", "expected"),
      (null, '*', null),
      ("", '*', Array.empty[String]),
      ("a.b.c", '.', Array("a", "b", "c")),
      ("a..b.c", '.', Array("a", "", "b", "c")),
      ("a:b:c", '.', Array("a:b:c")),
      //checkSplit("a\tb\nc", null, Array("a", "b", "c"), // null splits on whitespace
      ("a b c", ' ', Array("a", "b", "c")),
      ("a b c ", ' ', Array("a", "b", "c", "")),
      ("a b c  ", ' ', Array("a", "b", "c", "", "")),
      (" a b c", ' ', Array("", "a", "b", "c")),
      ("  a b c", ' ', Array("", "", "a", "b", "c")),
      (" a b c ", ' ', Array("", "a", "b", "c", ""))
    )

    forAll(data)((string: String, separator: Char, expected: Array[String]) =>
      val result: Array[String] = StringUtils.splitPreserveAllTokens(string, separator)
        assert(((result == null) && (expected == null)) || result.sameElements(expected))
    )

  "substringAfterLast" should "work" in:
    val data: TableFor3[String, String, String] = Table(
      ("string", "separator", "expected"),
      (null, "*", null),
      ("", "*", ""),
      ("*", "", ""),
      //("*"    , null, ""  ),
      ("abc", "a", "bc"),
      ("abcba", "b", "a"),
      ("abc", "c", ""),
      ("a", "a", ""),
      ("a", "z ", "")
    )

    forAll(data)((string: String, separator: String, expected: String) =>
      StringUtils.substringAfterLast(string, separator) shouldBe expected
    )
