package org.podval.tools.test.testproject

import org.podval.tools.build.Dependency
import org.podval.tools.util.Files
import java.io.File

final class TestProjectWriter(projectDir: File):
  private def write(
    path: Seq[String],
    content: String
  ): Unit = Files.write(
    content = content,
    file = Files.fileSeq(
      directory = projectDir,
      segments = path
    )
  )

  private def writeFragments(fileName: String, fragments: Seq[String]): Unit = write(
    path = Seq(fileName),
    content = fragments.filter(_.nonEmpty).mkString("\n\n")
  )

  private def writeSources(
    isTest: Boolean,
    sources: Seq[SourceFile]
  ): Unit = for sourceFile: SourceFile <- sources do write(
    path = TestProjectWriter.sourcesPath(isTest) ++ Seq(s"${sourceFile.name}.scala"),
    content = TestProjectWriter.addPackage(sourceFile.content)
  )
  
  def writeSettings(fragments: Seq[String]): Unit = writeFragments("settings.gradle", fragments)

  def writeBuild(fragments: Seq[String]): Unit = writeFragments("build.gradle", fragments)

  def writeProperties(properties: Seq[(String, String)]): Unit = write(
    path = Seq("gradle.properties"),
    content = properties.map { case (name, value) => s"$name=$value" }.mkString("\n")
  )

  def writeSources(fixtures: Seq[Fixture]): Unit =
    writeSources(isTest = false, fixtures.flatMap(_.mainSources))
    writeSources(isTest = true , fixtures.flatMap(_.testSources))

object TestProjectWriter:
  private def sourcesPath(isTest: Boolean): Seq[String] = Seq(
    "src",
    if isTest then "test" else "main",
    "scala",
    "org",
    "podval",
    "tools",
    "test"
  )
  
  private def addPackage(content: String): String =
    if content.contains("package ")
    then content
    else s"package ${ForClass.testPackage}\n\n" + content
