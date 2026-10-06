package org.podval.tools.build

import org.podval.tools.jvm.JvmBackend
import org.podval.tools.scalajs.ScalaJSBackend
import org.podval.tools.scalanative.ScalaNativeBackend
import org.podval.tools.test.testproject.{Fragments, TestProject}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3, TableFor5}
import java.io.File

class ArtifactSuffixTest extends AnyFlatSpec, Matchers, TableDrivenPropertyChecks:
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

  "the main jar" should "keep the suffix unless useArtifactSuffix is false" in :
    forAll(mainJars): (
      caseName: String,
      backend: Backend,
      scalaVersion: ScalaVersion.Known,
      turnSuffixOff: Boolean,
      jarFile: String
    ) =>
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
        Fragments.applyScalaJsPlugin,
        Fragments.scalaVersion(scalaVersion),
        "version = '1.2.3'",
        flag
      ))

      writer.writeProperties(Seq(Backend.property -> backend.name))

      project.build("clean", "jar")

      val libs: File = File(project.projectDir, "build/libs")
      Option(libs.list()).map(_.toSeq).getOrElse(Seq.empty) shouldBe Seq(jarFile)
