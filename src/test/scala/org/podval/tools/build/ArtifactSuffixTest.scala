package org.podval.tools.build

import org.podval.tools.jvm.JvmBackend
import org.podval.tools.scalajs.ScalaJSBackend
import org.podval.tools.scalanative.ScalaNativeBackend
import org.podval.tools.test.testproject.{Fragments, TestProject}
import org.podval.tools.util.Files
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3, TableFor5}
import java.io.File

class ArtifactSuffixTest extends AnyFlatSpec, Matchers, TableDrivenPropertyChecks:
  private val version: String = "1.2.3"
  private val scala3: ScalaVersion.Known = ScalaBinaryVersion.Scala3WithScala3Library.scalaVersionDefault
  private val scala213: ScalaVersion.Known = ScalaBinaryVersion.Scala2_13.scalaVersionDefault
  private val scala212: ScalaVersion.Known = ScalaBinaryVersion.Scala2_12.scalaVersionDefault

  private val suffixes: TableFor3[Backend, ScalaVersion.Known, String] = Table(
    ("backend", "scala", "expected"),
    (JvmBackend, scala3, "_3"),
    (JvmBackend, scala213, "_2.13"),
    (JvmBackend, scala212, "_2.12"),
    (ScalaJSBackend, scala3, "_sjs1_3"),
    (ScalaJSBackend, scala213, "_sjs1_2.13"),
    (ScalaJSBackend, scala212, "_sjs1_2.12"),
    (ScalaNativeBackend, scala3, "_native0.5_3"),
    (ScalaNativeBackend, scala213, "_native0.5_2.13"),
    (ScalaNativeBackend, scala212, "_native0.5_2.12")
  )

  "Artifact.suffix" should "follow the backend and the Scala binary version" in :
    forAll(suffixes): (backend: Backend, scalaVersion: ScalaVersion.Known, expected: String) =>
      Artifact.suffix(backend, ScalaLibrary.fromScalaVersion(scalaVersion)) shouldBe expected

  private val mainJars: TableFor5[String, Backend, ScalaVersion.Known, Boolean, String] = Table(
    ("caseName", "backend", "scala", "turnSuffixOff", "jarFile"),
    ("jvm-scala3-default", JvmBackend, scala3, false, "suffixlib_3-1.2.3.jar"),
    ("jvm-scala3-off", JvmBackend, scala3, true, "suffixlib-1.2.3.jar"),
    ("jvm-scala213-default", JvmBackend, scala213, false, "suffixlib_2.13-1.2.3.jar"),
    ("js-scala3-default", ScalaJSBackend, scala3, false, "suffixlib_sjs1_3-1.2.3.jar"),
    ("js-scala3-off", ScalaJSBackend, scala3, true, "suffixlib-1.2.3.jar"),
    ("native-scala3-default", ScalaNativeBackend, scala3, false, "suffixlib_native0.5_3-1.2.3.jar")
  )

  "jars, the POM, and module metadata" should "follow useArtifactSuffix" in :
    forAll(mainJars): (
      caseName: String,
      backend: Backend,
      scalaVersion: ScalaVersion.Known,
      turnSuffixOff: Boolean,
      jarFile: String
    ) =>
      val artifactId: String = jarFile.stripSuffix(s"-$version.jar")
      val sourcesFile: String = s"$artifactId-$version-sources.jar"
      val project: TestProject = writeProject(
        caseName = caseName,
        backend = backend,
        scalaVersion = scalaVersion,
        turnSuffixOff = turnSuffixOff,
        extraPluginIds = Seq("maven-publish"),
        extraBuild = Seq(libraryPublishing(custom = false))
      )

      project.build(
        "clean",
        "jar",
        "sourcesJar",
        "generatePomFileForLibraryPublication",
        "generateMetadataFileForLibraryPublication"
      )

      assertLibs(project, jarFile, sourcesFile)
      read(project, "build/publications/library/pom-default.xml") should include(
        s"<artifactId>$artifactId</artifactId>"
      )
      val module: String = read(project, "build/publications/library/module.json")
      module should include(s""""module": "$artifactId"""")
      module should include(s""""name": "$jarFile"""")
      module should include(s""""name": "$sourcesFile"""")

  "a custom artifactId" should "not gain the suffix" in :
    val artifactId: String = "suffixlib_3"
    val project: TestProject = writeProject(
      caseName = "jvm-scala3-custom",
      backend = JvmBackend,
      scalaVersion = scala3,
      turnSuffixOff = false,
      extraPluginIds = Seq("maven-publish"),
      extraBuild = Seq(libraryPublishing(custom = true))
    )

    project.build("clean", "jar", "generatePomFileForCustomPublication")

    File(project.projectDir, s"build/libs/$artifactId-$version.jar").isFile shouldBe true
    read(project, "build/publications/custom/pom-default.xml") should include(
      "<artifactId>custom</artifactId>"
    )

  private val pluginPublications: TableFor3[String, Boolean, String] = Table(
    ("caseName", "turnSuffixOff", "artifactId"),
    ("gradle-plugin-default", false, "suffixlib_3"),
    ("gradle-plugin-off", true, "suffixlib")
  )

  "Gradle plugin publications" should "suffix pluginMaven and leave the marker id alone" in :
    forAll(pluginPublications): (caseName: String, turnSuffixOff: Boolean, artifactId: String) =>
      val project: TestProject = writeProject(
        caseName = caseName,
        backend = JvmBackend,
        scalaVersion = scala3,
        turnSuffixOff = turnSuffixOff,
        extraPluginIds = Seq("java-gradle-plugin", "maven-publish"),
        extraBuild = Seq(
          """gradlePlugin {
            |  plugins {
            |    dummy {
            |      id = 'org.example.dummy'
            |      implementationClass = 'org.gradle.api.plugins.JavaLibraryPlugin'
            |    }
            |  }
            |}
            |""".stripMargin
        )
      )

      project.build(
        "clean",
        "generatePomFileForPluginMavenPublication",
        "generatePomFileForDummyPluginMarkerMavenPublication"
      )

      val marker: String = read(project, "build/publications/dummyPluginMarkerMaven/pom-default.xml")
      read(project, "build/publications/pluginMaven/pom-default.xml") should include(
        s"<artifactId>$artifactId</artifactId>"
      )
      marker should include("<artifactId>org.example.dummy.gradle.plugin</artifactId>")
      marker should include(s"<artifactId>$artifactId</artifactId>")

  private def writeProject(
    caseName: String,
    backend: Backend,
    scalaVersion: ScalaVersion.Known,
    turnSuffixOff: Boolean,
    extraPluginIds: Seq[String],
    extraBuild: Seq[String]
  ): TestProject =
    val project: TestProject = TestProject(Seq("artifact-suffix", caseName))
    val writer = project.writer(None)

    writer.writeSettings(Seq(
      Fragments.settingsManagement,
      Fragments.rootProjectName("suffixlib"),
      Fragments.includeScalaJsPluginBuild
    ))

    val flag: String =
      if !turnSuffixOff then ""
      else
        """scalaBackend {
          |  useArtifactSuffix = false
          |}
          |""".stripMargin

    writer.writeBuild(Seq(
      plugins(extraPluginIds*),
      Fragments.scalaVersion(scalaVersion),
      "group = 'org.example'",
      s"version = '$version'",
      flag
    ) ++ extraBuild)

    writer.writeProperties(Seq(Backend.property -> backend.name))
    project

  private def plugins(extraIds: String*): String =
    val added: String = extraIds.map(id => s"  id '$id'").mkString("\n")
    val insertion: String = if added.isEmpty then "" else added + "\n"
    Fragments.applyScalaJsPlugin.replace("}\n", s"$insertion}\n")

  private def libraryPublishing(custom: Boolean): String =
    val customPublication: String =
      if !custom then ""
      else
        """    custom(MavenPublication) {
          |      from components.java
          |      artifactId = 'custom'
          |    }
          |""".stripMargin

    s"""publishing {
       |  publications {
       |    library(MavenPublication) {
       |      from components.java
       |    }
       |$customPublication
       |  }
       |}
       |java.withSourcesJar()
       |""".stripMargin

  private def assertLibs(project: TestProject, files: String*): Unit =
    val libs: File = File(project.projectDir, "build/libs")
    Option(libs.list()).map(_.toSeq.sorted).getOrElse(Seq.empty) shouldBe files.sorted

  private def read(project: TestProject, path: String): String =
    Files.read(File(project.projectDir, path)).mkString("\n")
