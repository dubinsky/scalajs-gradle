package org.podval.tools.scalajs

import org.podval.tools.build.ScalaLibrary
import org.podval.tools.node.NodeDependency
import org.podval.tools.scalajs.js.ScalaJSDependencies
import org.podval.tools.testing.{Sbt, framework}
import org.podval.tools.util.Files
import java.io.File

// This writes versions of everything into an AsciiDoc file that the documentation uses;
// this way, the versions are guaranteed to be consistent - if this was run ;)
// I did not bother putting it into a separate module or into tests to avoid including it in the plugin jar - yet?

// GitHub stupidly disables AsciDoc includes are disabled in README;
// see https://github.com/github/markup/issues/1095.
// One include (of the `versions.adoc` in `README.adoc`.)
// is not enough to bother with AsciiDoctor Reducer (https://github.com/asciidoctor/asciidoctor-reducer),
// so I just patch the Readme.adoc...
object AsciiDocAttributes:
  private val versions: Seq[(String, Any)] = Seq(
    "gradle" -> "8.12",
    "plugin" -> "0.4.16",
    
    "scala" -> ScalaLibrary.Scala3.versionDefault,
    "scala2-minor" -> ScalaLibrary.Scala3.scala2versionMinor,
    "scala2" -> ScalaLibrary.Scala2.versionDefault13,
    
    "zinc" -> Sbt.versionDefault,
    "sbt-test-interface" -> Sbt.TestInterface.versionDefault,
    
    "scalajs" -> ScalaJSDependencies.versionDefault,
    "scalajs-dom" -> ScalaJSDependencies.DomSJS.versionDefault,
    "scalajs-env-jsdom-nodejs" -> ScalaJSDependencies.JSDomNodeJS.versionDefault,
    
    "node" -> NodeDependency.versionDefault,

    "junit" -> framework.JUnit4.jUnitVersion,
    "framework-junit4" -> framework.JUnit4.versionDefault,
    "framework-munit" -> framework.MUnit.versionDefault,
    "framework-scalacheck" -> framework.ScalaCheck.versionDefault,
    "framework-scalatest" -> framework.ScalaTest.versionDefault,
    "framework-spec2" -> framework.Specs2.versionDefault,
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
    val readmeContent: Seq[String] = Files.read(readmeFile)

    Files.write(readmeFile, toString(
      readmeContent.takeWhile(_ != includeBoundary) ++
      Seq(includeBoundary) ++
      versions.map((name, value) => s":version-$name: ${value.toString}") ++
      readmeContent.dropWhile(_ != includeBoundary).tail.dropWhile(_ != includeBoundary)
    ))  
