package org.podval.tools.test.filter

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3, TableFor4}
import TestFilterPatternMatch.*

// Based on org.gradle.api.internal.tasks.testing.filter.TestSelectionMatcherTest
//   see https://raw.githubusercontent.com/gradle/gradle/master/subprojects/testing-base/src/test/groovy/org/gradle/api/internal/tasks/testing/filter/TestSelectionMatcherTest.groovy
// Note that the wildcard '*' has no special understanding of the '.' package separator.
// It’s purely text based. So --tests *.SomeTestClass will match any package, regardless of its 'depth'.
class TestFilterPatternTest extends AnyFlatSpec, TableDrivenPropertyChecks, Matchers:  
  it should "calculate included methods" in:
    val data: TableFor3[String, String, Option[TestFilterPatternMatch]] = Table(
      ("input", "className", "match"),

      // all tests in a package
      ("some.package.*", "some.package.Foo", Some(Suite)),
      ("some.package*", "some.package.Foo", Some(Suite)),
      ("some.package*", "some.package.sub.Foo", Some(Suite)),
      ("some.package*", "some.package1.Foo", Some(Suite)),
      ("some.package*", "some.other.package.Foo", None),

      // all tests in some class
      ("SomeClass", "SomeClass", Some(Suite)),
      ("some.package.SomeClass", "some.package.SomeClass", Some(Suite)),

      // all tests in some class in any package
      ("SomeClass", "any.package.SomeClass", Some(Suite)),
      ("*.SomeClass", "any.package.SomeClass", Some(Suite)),

      // include all integration tests
      ("*IntegTest", "XIntegTest", Some(Suite)),

      // method in a package
      ("some.package*.someMethod", "some.package.Foo", Some(TestName("someMethod"))),
      ("some.package*.some*", "some.package.Foo", Some(TestWildcard("some"))),

      // single specified test in some class
      ("SomeClass.someMethod", "SomeClass", Some(TestName("someMethod"))),
      ("some.package.SomeClass.someMethod", "some.package.SomeClass", Some(TestName("someMethod"))),
      ("*SomeClass.someMethod", "SomeClass", Some(TestName("someMethod"))),

      // single specified test in some class in any package
      ("SomeClass.someMethod", "any.package.SomeClass", Some(TestName("someMethod"))),
      ("*.SomeClass.someMethod", "any.package.SomeClass", Some(TestName("someMethod"))),

      // specified tests in some class
      ("SomeClass.*someMethod*", "SomeClass", Some(TestWildcard("someMethod"))),

      // method name containing spaces
      ("org.gradle.SomeClass.some method containing spaces", "org.gradle.SomeClass", Some(TestName("some method containing spaces"))),
    )

    forAll(data): (input, className, expected) =>
      TestFilterPattern(input).matchClass(className) shouldBe expected
