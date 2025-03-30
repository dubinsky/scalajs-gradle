package org.podval.tools.test.filter

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3, TableFor4}

class TestFilterTest extends AnyFlatSpec, TableDrivenPropertyChecks, Matchers:
  it should "work with empty patterns" in :
    TestFilter(Set.empty, Set.empty, Set.empty).matchClass("anything").nonEmpty shouldBe true

  it should "calculate included methods" in:
    val data: TableFor3[String, String, Option[TestFilterMatch]] = Table(
      ("input", "className", "match"),

      // all tests in a package
      ("some.package.*", "some.package.Foo", Some(SuiteTestFilterMatch(false))),
      ("some.package*", "some.package.Foo", Some(SuiteTestFilterMatch(false))),
      ("some.package*", "some.package.sub.Foo", Some(SuiteTestFilterMatch(false))),
      ("some.package*", "some.package1.Foo", Some(SuiteTestFilterMatch(false))),
      ("some.package*", "some.other.package.Foo", None),

      // method in a package
      ("some.package*.someMethod", "some.package.Foo", Some(TestsTestFilterMatch(Set("someMethod"), Set.empty))),
      ("some.package*.some*", "some.package.Foo", Some(TestsTestFilterMatch(Set.empty, Set("some")))),

      // all tests in some class
      ("SomeClass", "SomeClass", Some(SuiteTestFilterMatch(false))),
      ("some.package.SomeClass", "some.package.SomeClass", Some(SuiteTestFilterMatch(false))),

      // all tests in some class in any package
      ("SomeClass", "any.package.SomeClass", Some(SuiteTestFilterMatch(false))),
      ("*.SomeClass", "any.package.SomeClass", Some(SuiteTestFilterMatch(false))),

      // single specified test in some class
      ("SomeClass.someMethod", "SomeClass", Some(TestsTestFilterMatch(Set("someMethod"), Set.empty))),
      ("some.package.SomeClass.someMethod", "some.package.SomeClass", Some(TestsTestFilterMatch(Set("someMethod"), Set.empty))),
      // TODO ("*SomeClass.someMethod", "SomeClass", Some(Tests(Set("someMethod"), Set.empty))),

      // single specified test in some class in any package
      ("SomeClass.someMethod", "any.package.SomeClass", Some(TestsTestFilterMatch(Set("someMethod"), Set.empty))),
      // TODO     ("*.SomeClass.someMethod", "any.package.SomeClass", Some(Tests(Set("someMethod"), Set.empty))),

      // specified tests in some class
      ("SomeClass.*someMethod*", "SomeClass", Some(TestsTestFilterMatch(Set.empty, Set("someMethod")))),

      // method name containing spaces
      ("org.gradle.SomeTestClass.some method containing spaces", "org.gradle.SomeTestClass", Some(TestsTestFilterMatch(Set("some method containing spaces"), Set.empty))),

      // include all integration tests
      ("*IntegTest", "XIntegTest", Some(SuiteTestFilterMatch(false))),

      // include specific method in any of the tests
      // TODO     ("*UiCheck", "Foo", Some(Matches.Tests(true, Set.empty, Set("UiCheck")))) // TODO [filter]
      //  //only ui tests from integration tests, by some naming convention
      //  "*IntegTest*ui"

      //# the second iteration of a parameterized test
      // '*ParameterizedTest.*[2]'
      // '*ParameterizedTest.foo*'
    )

    forAll(data): (input, className, expected) =>
      TestFilter(Set(input), Set.empty, Set.empty).matchClass(className) shouldBe expected

  it should "work with multiple patterns" in:
    val data: TableFor4[String, String, String, Boolean] = Table(
      ("pattern1", "pattern2", "className", "expected"),
      ("FooTest*", "FooTest", "FooTest", true),
      ("FooTest*", "BarTest*", "FooTest", false),
      ("FooTest*", "BarTest*", "FooBarTest", false),
      ("org.gradle.FooTest*", "org.gradle.BarTest*", "org.gradle.FooTest", false),
      ("org.gradle.FooTest*", "*org.gradle.BarTest*", "org.gradle.FooTest", true)
    )

    forAll(data): (pattern1: String, pattern2: String, className: String, expected: Boolean) =>
      TestFilter(Set(pattern1), Set.empty, Set(pattern2)).matchClass(className).nonEmpty shouldBe expected

    
