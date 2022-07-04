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

//      def "knows if test matches class"() {
//        expect: new TestSelectionMatcher(input, [], []).matchesTest(className, methodName) == match
//                new TestSelectionMatcher([], [], input).matchesTest(className, methodName) == match

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

  def 'can exclude as many classes as possible'() {
      expect:
      new TestSelectionMatcher(input, [], []).mayIncludeClass(fullQualifiedName) == maybeMatch
      new TestSelectionMatcher([], [], input).mayIncludeClass(fullQualifiedName) == maybeMatch

      where:
      input                             | fullQualifiedName    | maybeMatch
      ['.']                             | 'FooTest'            | false
      ['.FooTest.']                     | 'FooTest'            | false
      ['FooTest']                       | 'FooTest'            | true
      ['FooTest']                       | 'org.gradle.FooTest' | true
      ['FooTest']                       | 'org.foo.FooTest'    | true
      ['FooTest']                       | 'BarTest'            | false
      ['FooTest']                       | 'org.gradle.BarTest' | false
      ['FooTest.testMethod']            | 'FooTest'            | true
      ['FooTest.testMethod']            | 'BarTest'            | false
      ['FooTest.testMethod']            | 'org.gradle.FooTest' | true
      ['FooTest.testMethod']            | 'org.gradle.BarTest' | false
      ['org.gradle.FooTest.testMethod'] | 'FooTest'            | false
      ['org.gradle.FooTest.testMethod'] | 'org.gradle.FooTest' | true
      ['org.gradle.FooTest.testMethod'] | 'org.gradle.BarTest' | false
      ['org.foo.FooTest.testMethod']    | 'org.gradle.FooTest' | false
      ['org.foo.FooTest']               | 'org.gradle.FooTest' | false

      ['*FooTest*']                     | 'org.gradle.FooTest' | true
      ['*FooTest*']                     | 'aaa'                | true
      ['*FooTest']                      | 'org.gradle.FooTest' | true
      ['*FooTest']                      | 'FooTest'            | true
      ['*FooTest']                      | 'org.gradle.BarTest' | true // org.gradle.BarTest.testFooTest

      ['or*']                           | 'org.gradle.FooTest' | true
      ['org*']                          | 'org.gradle.FooTest' | true
      ['org.*']                         | 'org.gradle.FooTest' | true
      ['org.g*']                        | 'org.gradle.FooTest' | true
      ['org*']                          | 'FooTest'            | false
      ['org.*']                         | 'com.gradle.FooTest' | false
      ['org*']                          | 'com.gradle.FooTest' | false
      ['org.*']                         | 'com.gradle.FooTest' | false
      ['org.g*']                        | 'com.gradle.FooTest' | false
      ['FooTest*']                      | 'FooTest'            | true
      ['FooTest*']                      | 'org.gradle.FooTest' | true
      ['FooTest*']                      | 'BarTest'            | false
      ['FooTest*']                      | 'org.gradle.BarTest' | false
      ['org.gradle.FooTest*']           | 'org.gradle.BarTest' | false
      ['FooTest.testMethod*']           | 'FooTest'            | true
      ['FooTest.testMethod*']           | 'org.gradle.FooTest' | true
      ['org.foo.FooTest*']              | 'FooTest'            | false
      ['org.foo.FooTest*']              | 'org.gradle.FooTest' | false
      ['org.foo.*FooTest*']             | 'org.gradle.FooTest' | false
      ['org.foo.*FooTest*']             | 'org.foo.BarTest'    | true // org.foo.BarTest.testFooTest

      ['Foo']                           | 'FooTest'            | false
      ['org.gradle.Foo']                | 'org.gradle.FooTest' | false
      ['org.gradle.Foo.*']              | 'org.gradle.FooTest' | false

      ['org.gradle.Foo$Bar.*test']      | 'Foo'                | false
      ['org.gradle.Foo$Bar.*test']      | 'org.Foo'            | false
      ['org.gradle.Foo$Bar.*test']      | 'org.gradle.Foo'     | true
      ['Enclosing$Nested.test']         | "Enclosing"          | true
      ['org.gradle.Foo$1$2.test']       | "org.gradle.Foo"     | true
  }

  def 'can use multiple patterns'() {
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
