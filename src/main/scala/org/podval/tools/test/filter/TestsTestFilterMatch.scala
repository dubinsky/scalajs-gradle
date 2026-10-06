package org.podval.tools.test.filter

final case class TestsTestFilterMatch(
  testNames: Set[String],
  testWildcards: Set[String]
) extends TestFilterMatch:
  override def explicitlySpecified: Boolean = false

  override def isEmpty: Boolean = testNames.isEmpty && testWildcards.isEmpty

  override def intersect(other: TestFilterMatch): TestFilterMatch = other match
    case _: SuiteTestFilterMatch => this
    case that: TestsTestFilterMatch =>
      // sbt ORs selectors. A wildcard is a "contains" check. Keep an exact name when it
      // satisfies the other side, and keep a wildcard only when it contains one of the
      // other side's wildcards (it is that specific, or more). Two wildcards that do not
      // contain each other cannot be expressed as an AND, so they contribute nothing.
      def namesMatching(names: Set[String], wildcards: Set[String]): Set[String] =
        names.filter(name => wildcards.exists(name.contains))

      def narrowed(wildcards: Set[String], by: Set[String]): Set[String] =
        wildcards.filter(wildcard => by.exists(wildcard.contains))

      TestsTestFilterMatch(
        testNames = this.testNames.intersect(that.testNames) ++
          namesMatching(this.testNames, that.testWildcards) ++
          namesMatching(that.testNames, this.testWildcards),
        testWildcards = narrowed(this.testWildcards, that.testWildcards) ++
          narrowed(that.testWildcards, this.testWildcards)
      )
