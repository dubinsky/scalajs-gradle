package org.podval.tools.test

class MatchesTestTest

/*
it should "knows if test matches class" in {
  val data: TableFor4[String, String, String, Boolean] = Table(
    ("input", "className", "methodName", "expected"),
    ("FooTest", "FooTest", "whatever", true),
    ("FooTest", "fooTest", "whatever", false),

    ("com.foo.FooTest", "com.foo.FooTest", "x", true),
    ("com.foo.FooTest", "FooTest", "x", false),
    ("com.foo.FooTest", "com_foo_FooTest", "x", false),

    ("com.foo.FooTest.*", "com.foo.FooTest", "aaa", true),
    ("com.foo.FooTest.*", "com.foo.FooTest", "bbb", true),
    ("com.foo.FooTest.*", "com.foo.FooTestx", "bbb", false),

    ("*.FooTest.*", "com.foo.FooTest", "aaa", true),
    ("*.FooTest.*", "com.bar.FooTest", "aaa", true),
    ("*.FooTest.*", "FooTest", "aaa", false),

    ("com*FooTest", "com.foo.FooTest", "aaa", true),
    ("com*FooTest", "com.FooTest", "bbb", true),
    ("com*FooTest", "FooTest", "bbb", false),

    ("*.foo.*", "com.foo.FooTest","aaaa", true),
    ("*.foo.*", "com.foo.bar.BarTest", "aaaa", true),
    ("*.foo.*", "foo.Test", "aaaa", false),
    ("*.foo.*", "fooTest", "aaaa", false),
    ("*.foo.*", "foo", "aaaa", false)
  )

  forAll(data)((input, className, methodName, expected) =>
    TestFiltering(Set(input), Set.empty, Set.empty).matchesTest(className, methodName) shouldBe expected
    TestFiltering(Set.empty, Set.empty, Set(input)).matchesTest(className, methodName) shouldBe expected
  )
}

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
