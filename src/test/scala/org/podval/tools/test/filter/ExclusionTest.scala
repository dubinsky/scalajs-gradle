package org.podval.tools.test.filter

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3, TableFor4}

class ExclusionTest extends AnyFlatSpec, TableDrivenPropertyChecks, Matchers:
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

  "TestFilterPattern" should "exclude as many classes as possible" in :
    forAll(data): (input: String, className: String, expected: Boolean) =>
      TestFilterPattern(input).matchClass(className).isDefined shouldBe expected

  "TestFilter" should "exclude as many classes as possible" in :
    forAll(data): (input: String, className: String, expected: Boolean) =>
      assert(
        TestFilter(Set(input), Set.empty, Set.empty ).matchClass(className).nonEmpty === expected &&
        TestFilter(Set.empty , Set.empty, Set(input)).matchClass(className).nonEmpty === expected
      )
