package org.podval.tools.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3, TableFor4}
import org.scalatest.matchers.should.Matchers

// Note: heavily based on org.gradle.api.internal.tasks.testing.filter.TestSelectionMatcherTest
//   see https://raw.githubusercontent.com/gradle/gradle/master/subprojects/testing-base/src/test/groovy/org/gradle/api/internal/tasks/testing/filter/TestSelectionMatcherTest.groovy
class MatchesClassTest extends AnyFlatSpec, TableDrivenPropertyChecks, Matchers:

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

  it should "calculate included methods" in {
    import TestFilter.Matches

    val data: TableFor3[String, String, Option[Matches]] = Table(
      ("input", "className", "match"),

      // include specific method in any of the tests
//      ("*UiCheck", "Foo", Some(Matches.Tests(true, Set.empty, Set("UiCheck")))) // TODO

      // include all tests from package
      ("org.gradle.internal.*", "org.gradle.internal.Foo", Some(Matches.Suite(true))),

      // include all integration tests
      ("*IntegTest", "XIntegTest", Some(Matches.Suite(true))),

      // Executes all tests in SomeTestClass
      ("SomeTestClass", "SomeTestClass", Some(Matches.Suite(true))),

      // Executes a single specified test in SomeTestClass
      ("SomeTestClass.someSpecificMethod", "SomeTestClass", Some(Matches.Tests(Set("someSpecificMethod"), Set.empty))),

      // Executes specified tests in SomeTestClass
      ("SomeTestClass.*someMethod*", "SomeTestClass", Some(Matches.Tests(Set.empty, Set("someMethod")))),

      // method name containing spaces
      ("org.gradle.SomeTestClass.some method containing spaces", "org.gradle.SomeTestClass", Some(Matches.Tests(Set("some method containing spaces"), Set.empty))),

      // all classes at specific package (recursively)
      ("all.in.specific.package*", "all.in.specific.package.Foo", Some(Matches.Suite(true))),
      ("all.in.specific.package*", "all.in.specific.package.sub.Foo", Some(Matches.Suite(true))),
      ("all.in.specific.package*", "all.in.specific.package1.Foo", Some(Matches.Suite(true))),
      ("all.in.specific.package*", "all.in.another.package.Foo", None),

      // specific method at specific package (recursively)
      ("all.in.specific.package*.someSpecificMethod", "all.in.specific.package.Foo", Some(Matches.Tests(Set("someSpecificMethod"), Set.empty))),
      ("all.in.specific.package*.some*", "all.in.specific.package.Foo", Some(Matches.Tests(Set.empty, Set("some"))))

      // '*ParameterizedTest.foo*'
    )

    forAll(data) { (input, className, expected) =>
      TestFilter(Set(input), Set.empty, Set.empty).matchesClass(className) shouldBe expected
    }
  }

  // TODO when executed as a part of the class, it is not being reported AT ALL!?
  // Something with the maybeNested?
  it should "exclude as many classes as possible" in {
    val data: TableFor3[String, String, Boolean] = Table(
      ("input", "className", "expected"),
      (".", "FooTest", false),
      (".FooTest.", "FooTest", false),
      ("FooTest", "FooTest", true),
      ("FooTest", "org.gradle.FooTest", true),
      ("FooTest", "org.foo.FooTest", true),
      ("FooTest", "BarTest", false),
      ("FooTest", "org.gradle.BarTest", false),
      ("FooTest.testMethod", "FooTest", true),
      ("FooTest.testMethod", "BarTest", false),
      ("FooTest.testMethod", "org.gradle.FooTest", true),
      ("FooTest.testMethod", "org.gradle.BarTest", false),
      ("org.gradle.FooTest.testMethod", "FooTest", false),
      ("org.gradle.FooTest.testMethod", "org.gradle.FooTest", true),
      ("org.gradle.FooTest.testMethod", "org.gradle.BarTest", false),
      ("org.foo.FooTest.testMethod", "org.gradle.FooTest", false),
      ("org.foo.FooTest", "org.gradle.FooTest", false),

      ("*FooTest*", "org.gradle.FooTest", true),
      ("*FooTest*", "aaa", true),
      ("*FooTest", "org.gradle.FooTest", true),
      ("*FooTest", "FooTest", true),
      ("*FooTest", "org.gradle.BarTest", true), // org.gradle.BarTest.testFooTest

      ("or*", "org.gradle.FooTest", true),
      ("org*", "org.gradle.FooTest", true),
      ("org.*", "org.gradle.FooTest", true),
      ("org.g*", "org.gradle.FooTest", true),
      ("org*", "FooTest", false),
      ("org.*", "com.gradle.FooTest", false),
      ("org*", "com.gradle.FooTest", false),
      ("org.*", "com.gradle.FooTest", false),
      ("org.g*", "com.gradle.FooTest", false),
      ("FooTest*", "FooTest", true),
      ("FooTest*", "org.gradle.FooTest", true),
      ("FooTest*", "BarTest", false),
      ("FooTest*", "org.gradle.BarTest", false),
      ("org.gradle.FooTest*", "org.gradle.BarTest", false),
      ("FooTest.testMethod*", "FooTest", true),
      ("FooTest.testMethod*", "org.gradle.FooTest", true),
      ("org.foo.FooTest*", "FooTest", false),
      ("org.foo.FooTest*", "org.gradle.FooTest", false),
      ("org.foo.*FooTest*", "org.gradle.FooTest", false),
      ("org.foo.*FooTest*", "org.foo.BarTest", true), // org.foo.BarTest.testFooTest

      ("Foo", "FooTest", false),
      ("org.gradle.Foo", "org.gradle.FooTest", false),
      ("org.gradle.Foo.*", "org.gradle.FooTest", false),

      ("org.gradle.Foo$Bar.*test", "Foo", false),
      ("org.gradle.Foo$Bar.*test", "org.Foo", false),
      ("org.gradle.Foo$Bar.*test", "org.gradle.Foo", true),
      ("Enclosing$Nested.test", "Enclosing", true),
      ("org.gradle.Foo$1$2.test", "org.gradle.Foo", true)
    )

    forAll(data)((input: String, className: String, expected: Boolean) =>
      TestFilter(Set(input), Set.empty, Set.empty).mayIncludeClass(className) shouldBe expected
      TestFilter(Set.empty, Set.empty, Set(input)).mayIncludeClass(className) shouldBe expected
    )
  }

  it should "work with empty patterns" in {
    TestFilter(Set.empty, Set.empty, Set.empty).mayIncludeClass("anything") shouldBe true
  }

  it should "work with multiple patterns" in {
    val data: TableFor4[String, String, String, Boolean] = Table(
      ("pattern1", "pattern2", "className", "expected"),
      ("FooTest*", "FooTest", "FooTest", true),
      ("FooTest*", "BarTest*", "FooTest", false),
      ("FooTest*", "BarTest*", "FooBarTest", false),
      ("org.gradle.FooTest*", "org.gradle.BarTest*", "org.gradle.FooTest", false),
      ("org.gradle.FooTest*", "*org.gradle.BarTest*", "org.gradle.FooTest", true)
    )

    forAll(data) { (pattern1: String, pattern2: String, className: String, expected: Boolean) =>
      TestFilter(Set(pattern1), Set.empty, Set(pattern2)).mayIncludeClass(className) shouldBe expected
    }
  }
