package org.podval.tools.scalajsplugin

import org.podval.tools.build.ScalaVersion
import org.podval.tools.node.NodeDependency
import org.podval.tools.scalajs.ScalaJS
import org.podval.tools.test.{Sbt, framework}
import org.podval.tools.util.{Files, Strings}
import java.io.File

// This writes versions of everything into an AsciiDoc file that the documentation uses;
// this way, the versions are guaranteed to be consistent - if this was run ;)
// I did not bother putting it into a separate module or into tests to avoid including it in the plugin jar - yet?

// GitHub stupidly disables AsciDoc includes are disabled in README;
// see https://github.com/github/markup/issues/1095.
// One include (of the `versions.adoc` in `README.adoc`.)
// is not enough to bother with AsciiDoctor Reducer (https://github.com/asciidoctor/asciidoctor-reducer),
// so I just patch the Readme.adoc...
object VersionsWriter:
  private val versions: Seq[(String, Any)] = Seq(
    "gradle" -> "8.13",
    "plugin" -> "0.5.1",
    
    "scala" -> ScalaVersion.Scala3.versionDefault,
    "scala2-minor" -> ScalaVersion.Scala2.majorAndMinor,
    "scala2" -> ScalaVersion.Scala2.Scala213.versionDefault,
    
    "zinc" -> Sbt.Zinc.versionDefault,
    "sbt-test-interface" -> Sbt.TestInterface.versionDefault,
    
    "scalajs" -> ScalaJS.versionDefault,
    "scalajs-dom" -> ScalaJS.DomSJS.versionDefault,
    "scalajs-env-jsdom-nodejs" -> ScalaJS.JSDomNodeJS.versionDefault,
    
    "node" -> NodeDependency.versionDefault,

    "junit" -> framework.JUnit4Underlying.versionDefault,
    "framework-junit4" -> framework.JUnit4.versionDefault,
    "framework-junit4-scalajs" -> framework.JUnit4ScalaJS.versionDefault,
    "framework-munit" -> framework.MUnit.versionDefault,
    "framework-scalacheck" -> framework.ScalaCheck.versionDefault,
    "framework-scalatest" -> framework.ScalaTest.versionDefault,
    "framework-specs2" -> framework.Specs2.versionDefault,
    "framework-specs2-scala2" -> framework.Specs2.versionDefaultScala2.get,
    "framework-utest" -> framework.UTest.versionDefault,
    "framework-zio-test" -> framework.ZioTest.versionDefault
  )

  private val includeBoundary: String = "// INCLUDED ATTRIBUTES"

  def main(args: Array[String]): Unit =
    def toString(strings: Seq[String]): String = strings.mkString("", "\n", "\n")

    Files.write(File("gradle.properties").getAbsoluteFile, toString(
      Seq("org.podval.tools.scalajs.disabled = true") ++
        versions.map((name, value) =>
          s"version_${name.replace('-', '_')} = ${value.toString}"
        )
    ))

    val readmeFile: File = File("README.adoc").getAbsoluteFile

    Files.write(readmeFile, toString(Strings.splice(
      in = Files.read(readmeFile),
      boundary = includeBoundary,
      patch = versions.map((name, value) => s":version-$name: ${value.toString}")
    )))
