package org.podval.tools.scalajsplugin

import org.podval.tools.build.jvm.JvmBackend
import org.podval.tools.build.scalajs.ScalaJSBackend
import org.podval.tools.build.scalanative.ScalaNativeBackend
import org.podval.tools.build.{ScalaModules, ScalaBinaryVersion, Version}
import org.podval.tools.node.NodeDependency
import org.podval.tools.test.framework
import org.podval.tools.util.{Files, Strings}
import java.io.File

// This writes versions of everything into an AsciiDoc file that the documentation uses;
// this way, the versions are guaranteed to be consistent - if this was run ;)
// I did not bother putting it into a separate module or into tests to avoid including it in the plugin jar - yet?
object VersionsWriter:
  private val versions: Seq[(String, Version)] = Seq(
    "gradle" -> Version.Simple("8.14.1"),
    "plugin" -> Version.Simple("0.8.1"),
    
    "scala" -> ScalaBinaryVersion.Scala3.versionDefault.version,
    "scala213-suffix" -> ScalaBinaryVersion.Scala213.versionSuffix,
    "scala2" -> ScalaBinaryVersion.Scala213.versionDefault.version,
    
    "sbt-test-interface" -> JvmBackend.SbtTestInterface.versionDefault,
    
    "scalajs" -> ScalaJSBackend.versionDefault,
    "scalajs-dom" -> ScalaJSBackend.DomSJS.versionDefault,
    "scalajs-env-jsdom-nodejs" -> ScalaJSBackend.JSDomNodeJS.versionDefault,
    
    "node" -> NodeDependency.versionDefault,

    "scala-parallel-collections" -> ScalaModules.ParallelCollections.versionDefault,

    "scalanative" -> ScalaNativeBackend.versionDefault,
    
    "junit" -> framework.JUnit4Underlying.versionDefault,
    "framework-junit4" -> framework.JUnit4.versionDefault,
    "framework-junit4-scalajs" -> framework.JUnit4ScalaJS.versionDefault,
    "framework-junit4-scalanative" -> framework.JUnit4ScalaNative.versionDefault,
    "framework-munit" -> framework.MUnit.versionDefault,
    "framework-scalacheck" -> framework.ScalaCheck.versionDefault,
    "framework-scalatest" -> framework.ScalaTest.versionDefault,
    "framework-specs2" -> framework.Specs2.versionDefault,
    "framework-specs2-scala2" -> framework.Specs2.versionDefaultScala2.get,
    "framework-utest" -> framework.UTest.versionDefault,
    "framework-zio-test" -> framework.ZioTest.versionDefault
  )

  val attributes: Seq[(String, String)] = Seq(
    "pluginScalaBackendProperty"  -> ScalaJSPlugin.scalaBackendProperty,
    "pluginIncludeSharedProperty" -> ScalaJSPlugin.includeSharedProperty,
    "pluginScalaVersionProperty"  -> ScalaJSPlugin.scalaVersionProperty
  )
  
  def main(args: Array[String]): Unit =
    def toString(strings: Seq[String]): String = strings.mkString("", "\n", "\n")

    Files.write(File("gradle.properties").getAbsoluteFile, toString(
      versions.map((name, value) => s"version_${name.replace('-', '_')} = ${value.toString}")
    ))

    val readmeFile: File = File("README.adoc").getAbsoluteFile

    Files.write(readmeFile, toString(Strings.splice(
      in = Files.read(readmeFile),
      boundary = "// INCLUDED ATTRIBUTES",
      patch = 
        versions.map((name, version) => s":version-$name: ${version.toString}") ++
        attributes.map((name, value) => s":attribute-$name: $value")
    )))
