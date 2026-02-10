package org.podval.tools.backend

import org.podval.tools.build.{Backend, Dependency, ScalaBinaryVersion, TestFramework, Version}
import org.podval.tools.jvm.{JvmBackend, JUnit4Underlying}
import org.podval.tools.node.NodeInstaller
import org.podval.tools.scalajs.ScalaJSBackend
import org.podval.tools.scalanative.ScalaNativeBackend
import org.podval.tools.test.framework
import org.podval.tools.util.Files
import java.io.File

// This writes versions of everything into an AsciiDoc file that the documentation uses;
// this way, the versions are guaranteed to be consistent - if this was run ;)
object VersionsWriter:
  def main(args: Array[String]): Unit =
    Files.write(
      File("gradle.properties"),
      Seq(s"${Backend.property} = ${JvmBackend.name}") ++
      versions.map((name, version) => s"version_${name.replace('-', '_')} = $version")
    )

    Files.splice(
      file = File("README.adoc"),
      boundary = "// INCLUDED ATTRIBUTES",
      patch =
        attributes.map((name, value) => s":attribute-$name: $value") ++
        versions.map((name, version) => s":version-$name: $version")
    )

  private val gradleVersion: Version = Version("9.4.0-rc-2")
  private val pluginVersion: Version = Version("0.9.19")

  private def attributes: Seq[(String, String)] = Seq(
    "gradleVersionForBadge"    -> gradleVersion.toString.replace("-", "--"),
    "pluginBackendProperty"    -> Backend.property
  )

  private val versions: Seq[(String, Version)] = Seq(
    "gradle"                   -> gradleVersion,
    "plugin"                   -> pluginVersion,
    "scalanative"              -> ScalaNativeBackend.versionDefault,
    "scalajs"                  -> ScalaJSBackend    .versionDefault,
    "framework-specs2-scala2"  -> framework.Specs2  .versionDefaultScala2
  ) ++
    (dependencies ++ testFrameworks).map((name, dependency) => name -> dependency.versionDefault)

  private def dependencies: Seq[(String, Dependency)] = Seq(
    "scala"                    -> ScalaBinaryVersion.Scala3WithScala3Library,
    "scala-213"                -> ScalaBinaryVersion.Scala2_13,
    "scala-212"                -> ScalaBinaryVersion.Scala2_12,

    "sbt-test-interface"       -> JvmBackend.SbtTestInterface,

    "scalajs-dom"              -> ScalaJSBackend.dom,
    "scalajs-env-jsdom-nodejs" -> ScalaJSBackend.jsDomNode,
    "scala-js-env-playwright"  -> ScalaJSBackend.playwright,

    "node"                     -> NodeInstaller,

    "junit"                    -> JUnit4Underlying
  )

  private def testFrameworks: List[(String, Dependency)] = TestFramework
    .all
    .toList
    .map(framework => s"framework-${framework.name.toLowerCase.replace(" ", "-").replace(".", "-")}" ->
      framework.dependency
    )
