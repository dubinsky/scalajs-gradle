package org.podval.tools.build

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3, TableFor4}

class VersionTest extends AnyFlatSpec, Matchers, TableDrivenPropertyChecks:
  val data: TableFor3[String, String, Int] = Table(
    ("left", "right", "expected"),
    ("3.8.0", "3.8.0", 0),
    ("3.8", "3.8.0", -1),
    ("3.8", "3", 1)
  )

  "Version.compare" should "work" in :
    forAll(data): (left: String, right: String, expected: Int) =>
      Version(left).compare(Version(right)) shouldBe expected
