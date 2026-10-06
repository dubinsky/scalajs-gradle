package org.podval.tools.build

import org.podval.tools.jvm.JvmBackend
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3, TableFor4}

class VersionTest extends AnyFlatSpec, Matchers, TableDrivenPropertyChecks:
  val data: TableFor3[String, String, Int] = Table(
    ("left", "right", "expected"),
    ("3.8.0", "3.8.0", 0),
    ("3.8", "3.8.0", -1),
    ("3.8", "3", 1),
    ("3.8.0-RC1", "3.8.0", -1),
    ("3.8.0", "3.8.0-RC1", 1),
    ("3.10.0-RC1", "3.8.0", 1)
  )

  "Version.compare" should "work" in :
    forAll(data): (left: String, right: String, expected: Int) =>
      Version(left).compare(Version(right)) shouldBe expected

  "ScalaDependency.isScalaVersion" should "require a full Scala version for compiler plugins" in :
    val plugin: ScalaDependency = ScalaDependency(
      backend = JvmBackend,
      name = "compiler plugin",
      group = "org.scala-js",
      versionDefault = Version("1.22.0"),
      artifact = "scalajs-compiler",
      fullScalaVersion = true
    )
    val library: ScalaDependency = plugin.copy(fullScalaVersion = false)

    plugin.isScalaVersion(None) shouldBe false
    plugin.isScalaVersion(Some(Version("3"))) shouldBe false
    plugin.isScalaVersion(Some(Version("2.13"))) shouldBe false
    plugin.isScalaVersion(Some(Version("3.8.0"))) shouldBe true
    plugin.isScalaVersion(Some(Version("3.8.0-RC1"))) shouldBe true
    library.isScalaVersion(Some(Version("3"))) shouldBe true
    library.isScalaVersion(None) shouldBe false
