package org.podval.tools.test.testproject

import org.podval.tools.build.ScalaVersion

object Fragments:
  def settingsManagement: String =
    s"""pluginManagement {
       |  repositories {
       |    mavenLocal()
       |    mavenCentral()
       |    gradlePluginPortal()
       |  }
       |}
       |
       |dependencyResolutionManagement {
       |  repositories {
       |    mavenLocal()
       |    mavenCentral()
       |  }
       |}
       |""".stripMargin

  def rootProjectName(name: String): String = s"rootProject.name = '$name'"
  
  def includeScalaJsPluginBuild: String = s"includeBuild '${TestProject.root.getAbsolutePath}'"
       
  def includeSubprojects(subprojects: Seq[String]): String = prefixedList(s"include", subprojects)
  
  def applyScalaJsPlugin: String =
    s"""plugins {
       |  id 'org.podval.tools.scalajs' version '0.0.0'
       |}
       |""".stripMargin
       
  def continueOnFailure: String =
    s"""// Do not stop on test failures (needed when test tasks for multiple backends need to run):
       |gradle.startParameter.continueOnFailure = true
       |""".stripMargin

  def noJava: String =
    s"""// There is no Java in the project :)
       |project.gradle.startParameter.excludedTaskNames.add('compileJava')
       |""".stripMargin
       
  def scalaVersion(version: ScalaVersion): String = s"scala.scalaVersion = '$version'"

  def dependencies(
    implementation: Seq[String],
    testImplementation: Seq[String]
  ): String =
    s"""dependencies {
       |${prefixedList("  implementation", implementation)}
       |${prefixedList("  testImplementation", testImplementation)}
       |}
       |""".stripMargin

  def testTask(
    includeTestNames: Seq[String],
    excludeTestNames: Seq[String],
    includeTags: Seq[String],
    excludeTags: Seq[String],
    maxParallelForks: Int,
    more: Seq[String]
  ): String =
    s"""test {
       |  filter {
       |${prefixedList(s"    includeTestsMatching", includeTestNames)}
       |${prefixedList(s"    excludeTestsMatching", excludeTestNames)}
       |  }
       |  testLogging.lifecycle {
       |    events("STARTED", "PASSED", "SKIPPED", "FAILED", "STANDARD_OUT", "STANDARD_ERROR")
       |  }
       |  useSbt {
       |    includeCategories = ${includeTags.map(string => s"'$string'").mkString("[", ", ", "]")}
       |    excludeCategories = ${excludeTags.map(string => s"'$string'").mkString("[", ", ", "]")}
       |  }
       |  maxParallelForks = $maxParallelForks
       |${more.mkString("\n")}  
       |}
       |""".stripMargin

  private def prefixedList(prefix: String, list: Seq[String]): String = list
    .map(string => s"$prefix '$string'")
    .mkString("\n")
  