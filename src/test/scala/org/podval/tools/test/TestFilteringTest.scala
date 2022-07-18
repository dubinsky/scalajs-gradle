package org.podval.tools.test

import org.scalatest.funsuite.AnyFunSuite

//  include specific method in any of the tests
//  "*UiCheck"
//
//  include all tests from package
//  "org.gradle.internal.*"
//
//  include all integration tests
//  "*IntegTest"

//# Executes all tests in SomeTestClass
// "SomeTestClass"
//
//# Executes a single specified test in SomeTestClass
// "SomeTestClass.someSpecificMethod"
// "SomeTestClass.*someMethod*"

//# method name containing spaces
// "org.gradle.SomeTestClass.some method containing spaces"
//
//# all classes at specific package (recursively)
// 'all.in.specific.package*'
//
//# specific method at specific package (recursively)
// 'all.in.specific.package*.someSpecificMethod'
// '*IntegTest'
// '*IntegTest*ui*'
// '*ParameterizedTest.foo*'
//
//# the second iteration of a parameterized test
// '*ParameterizedTest.*[2]'

//Note that the wildcard '*' has no special understanding of the '.' package separator.
// It’s purely text based. So --tests *.SomeTestClass will match any package, regardless of its 'depth'.

//  //specific test class, this can match 'SomeTest' class and corresponding method under any package
//  "SomeTest"
//  "SomeTest.someTestMethod*"
//
//  //specific test class
//  "org.gradle.SomeTest"
//
//  //specific test class and method
//  "org.gradle.SomeTest.someSpecificFeature"
//
//  //specific test method, use wildcard
//  "*SomeTest.someSpecificFeature"
//
//  //specific test class, wildcard for packages
//  "*.SomeTest"
//
//  //all classes in package, recursively
//  "com.gradle.tooling.*"
//
//  //all integration tests, by naming convention
//  "*IntegTest"
//
//  //only ui tests from integration tests, by some naming convention
//  "*IntegTest*ui"


// Note: heavily based on org.gradle.api.internal.tasks.testing.filter.TestSelectionMatcherTest
//   see https://raw.githubusercontent.com/gradle/gradle/master/subprojects/testing-base/src/test/groovy/org/gradle/api/internal/tasks/testing/filter/TestSelectionMatcherTest.groovy
class TestFilteringTest extends AnyFunSuite:

  private def checkMatch(input: String, className: String, methodName: String, expected: Boolean): Unit =
    assert(expected == TestFiltering(Set(input), Set.empty, Set.empty)
      .matchesTest(className, methodName))

    assert(expected == TestFiltering(Set.empty, Set.empty, Set(input))
      .matchesTest(className, methodName))

  test("knows if test matches class") {
    checkMatch("FooTest", "FooTest", "whatever", true)
    checkMatch("FooTest", "fooTest", "whatever", false)

    checkMatch("com.foo.FooTest", "com.foo.FooTest", "x", true)
    checkMatch("com.foo.FooTest", "FooTest", "x", false)
    checkMatch("com.foo.FooTest", "com_foo_FooTest", "x", false)

    checkMatch("com.foo.FooTest.*", "com.foo.FooTest", "aaa", true)
    checkMatch("com.foo.FooTest.*", "com.foo.FooTest", "bbb", true)
    checkMatch("com.foo.FooTest.*", "com.foo.FooTestx", "bbb", false)

    checkMatch("*.FooTest.*", "com.foo.FooTest", "aaa", true)
    checkMatch("*.FooTest.*", "com.bar.FooTest", "aaa", true)
    checkMatch("*.FooTest.*", "FooTest", "aaa", false)

    checkMatch("com*FooTest", "com.foo.FooTest", "aaa", true)
    checkMatch("com*FooTest", "com.FooTest", "bbb", true)
    checkMatch("com*FooTest", "FooTest", "bbb", false)

    checkMatch("*.foo.*", "com.foo.FooTest", "aaaa", true)
    checkMatch("*.foo.*", "com.foo.bar.BarTest", "aaaa", true)
    checkMatch("*.foo.*", "foo.Test", "aaaa", false)
    checkMatch("*.foo.*", "fooTest", "aaaa", false)
    checkMatch("*.foo.*", "foo", "aaaa", false)
  }
  /*

  def "knows if excluded test matches class"() {
      expect: new TestSelectionMatcher([], input, []).matchesTest(className, methodName) == match

      where:
      input                    | className                 | methodName            | match
      ["FooTest"]              | "FooTest"                 | "whatever"            | false
      ["FooTest"]              | "fooTest"                 | "whatever"            | true

      ["com.foo.FooTest"]      | "com.foo.FooTest"         | "x"                   | false
      ["com.foo.FooTest"]      | "FooTest"                 | "x"                   | true
      ["com.foo.FooTest"]      | "com_foo_FooTest"         | "x"                   | true

      ["com.foo.FooTest.*"]    | "com.foo.FooTest"         | "aaa"                 | false
      ["com.foo.FooTest.*"]    | "com.foo.FooTest"         | "bbb"                 | false
      ["com.foo.FooTest.*"]    | "com.foo.FooTestx"        | "bbb"                 | true

      ["*.FooTest.*"]          | "com.foo.FooTest"         | "aaa"                 | false
      ["*.FooTest.*"]          | "com.bar.FooTest"         | "aaa"                 | false
      ["*.FooTest.*"]          | "FooTest"                 | "aaa"                 | true

      ["com*FooTest"]          | "com.foo.FooTest"         | "aaa"                 | false
      ["com*FooTest"]          | "com.FooTest"             | "bbb"                 | false
      ["com*FooTest"]          | "FooTest"                 | "bbb"                 | true

      ["*.foo.*"]              | "com.foo.FooTest"         | "aaaa"                | false
      ["*.foo.*"]              | "com.foo.bar.BarTest"     | "aaaa"                | false
      ["*.foo.*"]              | "foo.Test"                | "aaaa"                | true
      ["*.foo.*"]              | "fooTest"                 | "aaaa"                | true
      ["*.foo.*"]              | "foo"                     | "aaaa"                | true
  }

  def "knows if test matches"() {
      expect: new TestSelectionMatcher(input, [], []).matchesTest(className, methodName) == match
              new TestSelectionMatcher([], [], input).matchesTest(className, methodName) == match

      where:
      input                    | className                 | methodName            | match
      ["FooTest.test"]         | "FooTest"                 | "test"                | true
      ["FooTest.test"]         | "Footest"                 | "test"                | false
      ["FooTest.test"]         | "FooTest"                 | "TEST"                | false
      ["FooTest.test"]         | "com.foo.FooTest"         | "test"                | true
      ["FooTest.test"]         | "Foo.test"                | ""                    | false

      ["FooTest.*slow*"]       | "FooTest"                 | "slowUiTest"          | true
      ["FooTest.*slow*"]       | "FooTest"                 | "veryslowtest"        | true
      ["FooTest.*slow*"]       | "FooTest.SubTest"         | "slow"                | false
      ["FooTest.*slow*"]       | "FooTest"                 | "a slow test"         | true
      ["FooTest.*slow*"]       | "FooTest"                 | "aslow"               | true
      ["FooTest.*slow*"]       | "com.foo.FooTest"         | "slowUiTest"          | true
      ["FooTest.*slow*"]       | "FooTest"                 | "verySlowTest"        | false

      ["com.FooTest***slow*"]  | "com.FooTest"             | "slowMethod"          | true
      ["com.FooTest***slow*"]  | "com.FooTest2"            | "aslow"               | true
      ["com.FooTest***slow*"]  | "com.FooTest.OtherTest"   | "slow"                | true
      ["com.FooTest***slow*"]  | "FooTest"                 | "slowMethod"          | false
  }

  def "matches any of input"() {
      expect: new TestSelectionMatcher(input, [], []).matchesTest(className, methodName) == match
              new TestSelectionMatcher([], [], input).matchesTest(className, methodName) == match

      where:
      input                               | className                 | methodName            | match
      ["FooTest.test", "FooTest.bar"]     | "FooTest"                 | "test"                | true
      ["FooTest.test", "FooTest.bar"]     | "FooTest"                 | "bar"                 | true
      ["FooTest.test", "FooTest.bar"]     | "FooTest"                 | "baz"                 | false
      ["FooTest.test", "FooTest.bar"]     | "Footest"                 | "test"                | false

      ["FooTest.test", "BarTest.*"]       | "FooTest"                 | "test"                | true
      ["FooTest.test", "BarTest.*"]       | "BarTest"                 | "xxxx"                | true
      ["FooTest.test", "BarTest.*"]       | "FooTest"                 | "xxxx"                | false

      ["FooTest.test", "FooTest.*fast*"]  | "FooTest"                 | "test"                | true
      ["FooTest.test", "FooTest.*fast*"]  | "FooTest"                 | "fast"                | true
      ["FooTest.test", "FooTest.*fast*"]  | "FooTest"                 | "a fast test"         | true
      ["FooTest.test", "FooTest.*fast*"]  | "FooTest"                 | "xxxx"                | false

      ["FooTest", "*BarTest"]             | "FooTest"                 | "test"                | true
      ["FooTest", "*BarTest"]             | "FooTest"                 | "xxxx"                | true
      ["FooTest", "*BarTest"]             | "BarTest"                 | "xxxx"                | true
      ["FooTest", "*BarTest"]             | "com.foo.BarTest"         | "xxxx"                | true
      ["FooTest", "*BarTest"]             | "com.foo.FooTest"         | "xxxx"                | true
  }

  def "regexp chars are handled"() {
      expect: new TestSelectionMatcher(input, [], []).matchesTest(className, methodName) == match
              new TestSelectionMatcher([], [], input).matchesTest(className, methodName) == match

      where:
      input                               | className                 | methodName            | match
      ["*Foo+Bar*"]                       | "Foo+Bar"                 | "test"                | true
      ["*Foo+Bar*"]                       | "Foo+Bar"                 | "xxxx"                | true
      ["*Foo+Bar*"]                       | "com.Foo+Bar"             | "xxxx"                | true
      ["*Foo+Bar*"]                       | "FooBar"                  | "xxxx"                | false
  }

  def "handles null test method"() {
      expect: new TestSelectionMatcher(input, [], []).matchesTest(className, methodName) == match
              new TestSelectionMatcher([], [], input).matchesTest(className, methodName) == match

      where:
      input                               | className                 | methodName            | match
      ["FooTest"]                         | "FooTest"                 | null                  | true
      ["FooTest*"]                        | "FooTest"                 | null                  | true

      ["FooTest.*"]                       | "FooTest"                 | null                  | false
      ["FooTest"]                         | "OtherTest"               | null                  | false
      ["FooTest.test"]                    | "FooTest"                 | null                  | false
      ["FooTest.null"]                    | "FooTest"                 | null                  | false
  }

  def "script includes and command line includes both have to match"() {
      expect: new TestSelectionMatcher(input, [], inputCommandLine).matchesTest(className, methodName) == match

      where:
      input               | inputCommandLine | className  | methodName | match
      ["FooTest", "Bar" ] | []               | "FooTest"  | "whatever" | true
      ["FooTest"]         | ["Bar"]          | "FooTest"  | "whatever" | false
  }
*/

  private def checkMayInclude(input: String, className: String, expected: Boolean): Unit =
    assert(expected == TestFiltering(Set(input), Set.empty, Set.empty)
      .mayIncludeClass(className))

    assert(expected == TestFiltering(Set.empty, Set.empty, Set(input))
      .mayIncludeClass(className))

    test("can exclude as many classes as possible") {
      checkMayInclude(".", "FooTest", false)
      checkMayInclude(".FooTest.", "FooTest", false)
      checkMayInclude("FooTest", "FooTest", true)
      checkMayInclude("FooTest", "org.gradle.FooTest", true)
      checkMayInclude("FooTest", "org.foo.FooTest", true)
      checkMayInclude("FooTest", "BarTest", false)
      checkMayInclude("FooTest", "org.gradle.BarTest", false)
      checkMayInclude("FooTest.testMethod", "FooTest", true)
      checkMayInclude("FooTest.testMethod", "BarTest", false)
      checkMayInclude("FooTest.testMethod", "org.gradle.FooTest", true)
      checkMayInclude("FooTest.testMethod", "org.gradle.BarTest", false)
      checkMayInclude("org.gradle.FooTest.testMethod", "FooTest", false)
      checkMayInclude("org.gradle.FooTest.testMethod", "org.gradle.FooTest", true)
      checkMayInclude("org.gradle.FooTest.testMethod", "org.gradle.BarTest", false)
      checkMayInclude("org.foo.FooTest.testMethod", "org.gradle.FooTest", false)
      checkMayInclude("org.foo.FooTest", "org.gradle.FooTest", false)

      checkMayInclude("*FooTest*", "org.gradle.FooTest", true)
      checkMayInclude("*FooTest*", "aaa", true)
      checkMayInclude("*FooTest", "org.gradle.FooTest", true)
      checkMayInclude("*FooTest", "FooTest", true)
      checkMayInclude("*FooTest", "org.gradle.BarTest", true) // org.gradle.BarTest.testFooTest

      checkMayInclude("or*", "org.gradle.FooTest", true)
      checkMayInclude("org*", "org.gradle.FooTest", true)
      checkMayInclude("org.*", "org.gradle.FooTest", true)
      checkMayInclude("org.g*", "org.gradle.FooTest", true)
      checkMayInclude("org*", "FooTest", false)
      checkMayInclude("org.*", "com.gradle.FooTest", false)
      checkMayInclude("org*", "com.gradle.FooTest", false)
      checkMayInclude("org.*", "com.gradle.FooTest", false)
      checkMayInclude("org.g*", "com.gradle.FooTest", false)
      checkMayInclude("FooTest*", "FooTest", true)
      checkMayInclude("FooTest*", "org.gradle.FooTest", true)
      checkMayInclude("FooTest*", "BarTest", false)
      checkMayInclude("FooTest*", "org.gradle.BarTest", false)
      checkMayInclude("org.gradle.FooTest*", "org.gradle.BarTest", false)
      checkMayInclude("FooTest.testMethod*", "FooTest", true)
      checkMayInclude("FooTest.testMethod*", "org.gradle.FooTest", true)
      checkMayInclude("org.foo.FooTest*", "FooTest", false)
      checkMayInclude("org.foo.FooTest*", "org.gradle.FooTest", false)
      checkMayInclude("org.foo.*FooTest*", "org.gradle.FooTest", false)
      checkMayInclude("org.foo.*FooTest*", "org.foo.BarTest", true) // org.foo.BarTest.testFooTest

      checkMayInclude("Foo", "FooTest", false)
      checkMayInclude("org.gradle.Foo", "org.gradle.FooTest", false)
      checkMayInclude("org.gradle.Foo.*", "org.gradle.FooTest", false)

      checkMayInclude("org.gradle.Foo$Bar.*test", "Foo", false)
      checkMayInclude("org.gradle.Foo$Bar.*test", "org.Foo", false)
      checkMayInclude("org.gradle.Foo$Bar.*test", "org.gradle.Foo", true)
      checkMayInclude("Enclosing$Nested.test", "Enclosing", true)
      checkMayInclude("org.gradle.Foo$1$2.test", "org.gradle.Foo", true)
    }

  /*
  def "can use multiple patterns'() {
      expect:
      new TestSelectionMatcher(pattern1, [], pattern2).mayIncludeClass(fullQualifiedName) == maybeMatch

      where:
      pattern1                | pattern2                 | fullQualifiedName    | maybeMatch
      ['FooTest*']            | ['FooTest']              | 'FooTest'            | true
      ['FooTest*']            | ['BarTest*']             | 'FooTest'            | false
      ['FooTest*']            | ['BarTest*']             | 'FooBarTest'         | false
      []                      | []                       | 'anything'           | true
      ['org.gradle.FooTest*'] | ['org.gradle.BarTest*']  | 'org.gradle.FooTest' | false
      ['org.gradle.FooTest*'] | ['*org.gradle.BarTest*'] | 'org.gradle.FooTest' | true
  }
}
*/
  private def checkSplit(string: String, separator: Char, expected: Array[String]): Unit =
    val result: Array[String] = TestFiltering.splitPreserveAllTokens(string, separator)
    assert(((result == null) && (expected == null)) || result.sameElements(expected))

  test("splitPreserveAllTokens") {
    checkSplit(string = null     , separator = '*', expected = null)
    checkSplit(string = ""       , separator = '*', expected = Array.empty)
    checkSplit(string = "a.b.c"  , separator = '.', expected = Array("a", "b", "c"))
    checkSplit(string = "a..b.c" , separator = '.', expected = Array("a", "", "b", "c"))
    checkSplit(string = "a:b:c"  , separator = '.', expected = Array("a:b:c"))
    //checkSplit("a\tb\nc", null, Array("a", "b", "c") // null splits on whitespace
    checkSplit(string = "a b c"  , separator = ' ', expected = Array("a", "b", "c"))
    checkSplit(string = "a b c " , separator = ' ', expected = Array("a", "b", "c", ""))
    checkSplit(string = "a b c  ", separator = ' ', expected = Array("a", "b", "c", "", ""))
    checkSplit(string = " a b c" , separator = ' ', expected = Array("", "a", "b", "c"))
    checkSplit(string = "  a b c", separator = ' ', expected = Array("", "", "a", "b", "c"))
    checkSplit(string = " a b c ", separator = ' ', expected = Array("", "a", "b", "c", ""))
  }

  private def checkAfter(string: String, separator: String, expected: String): Unit =
    val result: String = TestFiltering.substringAfterLast(string, separator)
    assert(result == expected)

  test("substringAfterLast") {
    checkAfter(string = null, separator = "*", expected = null)
    checkAfter(string = "", separator = "*", expected = "")
    checkAfter(string = "*", separator = "", expected = "")
    //checkAfter(string = "*", separator = null, expected = "")
    checkAfter(string = "abc", separator = "a", expected = "bc")
    checkAfter(string = "abcba", separator = "b", expected = "a")
    checkAfter(string = "abc", separator = "c", expected = "")
    checkAfter(string = "a", separator = "a", expected = "")
    checkAfter(string = "a", separator = "z", expected = "")
  }
