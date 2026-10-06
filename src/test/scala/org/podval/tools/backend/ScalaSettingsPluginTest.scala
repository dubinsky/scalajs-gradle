package org.podval.tools.backend

import org.podval.tools.build.{Backend, ScalaBinaryVersion}
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.test.testproject.{Fragments, TestProject}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.io.File

// Settings plugins resolve before the rest of settings.gradle, so includeBuild goes in pluginManagement.
class ScalaSettingsPluginTest extends AnyFlatSpec, Matchers:
  private val linkTask: String = "link - Links Scala.js code."
  private val scala3 = ScalaBinaryVersion.Scala3WithScala3Library.scalaVersionDefault

  "scalaBackend.backend" should "select the Scala.js backend" in:
    val output: String = project(caseName = "backend-js", backend = Some("js")).build("tasks", "--all")
    output should include(linkTask)

  "the project property" should "override scalaBackend.backend" in:
    val testProject: TestProject = project(caseName = "property-wins", backend = Some("js"))
    testProject.writer(None).writeProperties(Seq(Backend.property -> JvmBackend.name))
    testProject.build("tasks", "--all") should not include linkTask

  "includeBackendProjects" should "include backend directories and leave other directories out" in:
    val testProject: TestProject = project(
      caseName = "include",
      backend = None,
      includeCore = true,
      applyProjectPlugin = false
    )
    Seq("js", "src", "core/js", "core/src").foreach(path => File(testProject.projectDir, path).mkdirs())

    projectPaths(testProject.build("projects")) shouldBe Set(":js", ":core", ":core:js")

  private def project(
    caseName: String,
    backend: Option[String],
    includeCore: Boolean = false,
    applyProjectPlugin: Boolean = true
  ): TestProject =
    val testProject: TestProject = TestProject(Seq("settings-plugin", caseName))
    val writer = testProject.writer(None)
    writer.writeSettings(Seq(settings(backend, includeCore)))
    // The settings plugin and the project plugin are the same jar. Once the settings plugin is applied,
    // the project plugin is already on the classpath with no recorded version, so a versioned request
    // is rejected. A Portal request of the same version for both ids does not hit that case.
    if applyProjectPlugin then writer.writeBuild(Seq(
      """plugins {
        |  id 'org.podval.tools.scalajs'
        |}
        |""".stripMargin,
      Fragments.scalaVersion(scala3)
    ))
    testProject

  private def settings(backend: Option[String], includeCore: Boolean): String =
    val backendLine: String = backend.fold("")(name => s"scalaBackend.backend = '$name'")
    val includeCoreLine: String = if includeCore then "include 'core'" else ""
    val includeBackendLine: String = if includeCore then "scalaBackend.includeBackendProjects()" else ""

    // plugins {} must follow pluginManagement directly. Other statements before it are rejected.
    s"""pluginManagement {
       |  includeBuild '${TestProject.root.getAbsolutePath}'
       |  repositories {
       |    mavenLocal()
       |    mavenCentral()
       |    gradlePluginPortal()
       |  }
       |}
       |
       |plugins {
       |  id 'org.podval.tools.scala.settings' version '0.0.0'
       |}
       |
       |dependencyResolutionManagement {
       |  repositories {
       |    mavenLocal()
       |    mavenCentral()
       |  }
       |}
       |
       |${Fragments.rootProjectName("settings-backend")}
       |$includeCoreLine
       |$backendLine
       |$includeBackendLine
       |""".stripMargin

  // `projects` prints `Project ':core:js'`. `:core:js` contains the characters `:js`, so match whole paths.
  private def projectPaths(output: String): Set[String] = output
    .linesIterator
    .flatMap(_.split("Project '", -1).drop(1))
    .map(_.takeWhile(_ != '\''))
    .toSet
