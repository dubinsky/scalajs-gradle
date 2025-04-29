package org.podval.tools.scalajsplugin

import org.podval.tools.build.{ScalaModules, ScalaVersion, Version}
import org.podval.tools.node.NodeDependency
import org.podval.tools.scalajs.ScalaJS
import org.podval.tools.test.{SbtTestInterface, framework}
import org.podval.tools.util.{Files, Strings}
import java.io.File

// This writes versions of everything into an AsciiDoc file that the documentation uses;
// this way, the versions are guaranteed to be consistent - if this was run ;)
// I did not bother putting it into a separate module or into tests to avoid including it in the plugin jar - yet?
object VersionsWriter:
  private val versions: Seq[(String, Version)] = Seq(
    "gradle" -> Version("8.14"),
    "plugin" -> Version("0.6.3"),
    
    "scala" -> ScalaVersion.Scala3.versionDefault,
    "scala2-minor" -> ScalaVersion.Scala2.majorAndMinor,
    "scala2" -> ScalaVersion.Scala2.Scala213.versionDefault,

    "scala-parallel-collections" -> ScalaModules.ParallelCollections.versionDefault,

    "sbt-test-interface" -> SbtTestInterface.versionDefault,
    
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

  val attributes: Seq[(String, String)] = Seq(
    "scalajsModeProperty" -> ScalaJSPlugin.modeProperty,
    "maiflaiProperty" -> ScalaJSPlugin.maiflaiProperty,
    "mixedModeName" -> ScalaJSPlugin.mixedModeName
  )
  
  def main(args: Array[String]): Unit =
    def toString(strings: Seq[String]): String = strings.mkString("", "\n", "\n")

    Files.write(File("gradle.properties").getAbsoluteFile, toString(
      Seq(s"${ScalaJSPlugin.modeProperty} = ${BackendDelegateKind.JVM.name}") ++
        versions.map((name, value) =>
          s"version_${name.replace('-', '_')} = ${value.toString}"
        )
    ))

    val readmeFile: File = File("README.adoc").getAbsoluteFile

    Files.write(readmeFile, toString(Strings.splice(
      in = Files.read(readmeFile),
      boundary = "// INCLUDED ATTRIBUTES",
      patch = 
        versions.map((name, version) => s":version-$name: ${version.toString}") ++
        attributes.map((name, value) => s":attribute-$name: $value")
    )))
