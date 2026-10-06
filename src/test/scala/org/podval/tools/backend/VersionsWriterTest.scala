package org.podval.tools.backend

import org.podval.tools.util.Files
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class VersionsWriterTest extends AnyFlatSpec, Matchers:
  "gradle.properties" should "match VersionsWriter" in:
    Files.read(VersionsWriter.gradlePropertiesFile.getAbsoluteFile) shouldBe
      VersionsWriter.gradlePropertiesLines

  "the README attribute block" should "match VersionsWriter" in:
    val lines: Seq[String] = Files.read(VersionsWriter.readmeFile.getAbsoluteFile)
    val patch: Seq[String] = lines
      .dropWhile(_ != VersionsWriter.readmeBoundary)
      .tail
      .takeWhile(_ != VersionsWriter.readmeBoundary)
    patch shouldBe VersionsWriter.readmeAttributeLines
