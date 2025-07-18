package org.podval.tools.backend

import org.podval.tools.build.{ScalaBinaryVersion, Version}
import org.podval.tools.jvm.{JvmBackend, JvmDependency}
import org.podval.tools.node.NodeDependency
import org.podval.tools.scalajs.{PlaywrightJSEnv, ScalaJSBackend}
import org.podval.tools.scalanative.ScalaNativeBackend
import org.podval.tools.test.framework
import org.podval.tools.util.{Files, Strings}
import java.io.File

// This writes versions of everything into an AsciiDoc file that the documentation uses;
// this way, the versions are guaranteed to be consistent - if this was run ;)
// I did not bother putting it into a separate module or into tests to avoid including it in the plugin jar - yet?
object VersionsWriter:
  private val gradleVersion: Version = Version("9.0.0-rc-3")

  private val versions: Seq[(String, Version)] = Seq(
    "plugin" -> Version("0.9.3"),

    "gradle" -> gradleVersion,
    
    "scala" -> ScalaBinaryVersion.Scala3.scalaVersionDefault.version,

    "sbt-test-interface" -> JvmDependency.SbtTestInterface.versionDefault,
    
    "scalajs" -> ScalaJSBackend.versionDefault,
    "scalajs-dom" -> ScalaJSBackend.DomSJS.versionDefault,
    "scalajs-env-jsdom-nodejs" -> ScalaJSBackend.JSDomNodeJSEnv.versionDefault,
    "scala-js-env-playwright" -> PlaywrightJSEnv.versionDefault,
    
    "node" -> NodeDependency.maker.versionDefault,

    "scalanative" -> ScalaNativeBackend.versionDefault,
    
    "junit" -> framework.JUnit4Underlying.versionDefault,
    "framework-junit4" -> framework.JUnit4.versionDefault,
    "framework-junit4-scalajs" -> framework.JUnit4ScalaJS.versionDefault,
    "framework-junit4-scalanative" -> framework.JUnit4ScalaNative.versionDefault,
    "framework-munit" -> framework.MUnit.versionDefault,
    "framework-scalacheck" -> framework.ScalaCheck.versionDefault,
    "framework-scalatest" -> framework.ScalaTest.versionDefault,
    "framework-specs2" -> framework.Specs2.versionDefault,
    "framework-specs2-scala2" -> framework.Specs2.versionDefaultScala2,
    "framework-utest" -> framework.UTest.versionDefault,
    "framework-zio-test" -> framework.ZioTest.versionDefault
  )

  val attributes: Seq[(String, String)] = Seq(
    "pluginScalaBackendProperty"  -> BackendPlugin.scalaBackendProperty,
    "pluginBuildPerScalaVersionProperty" -> BackendPlugin.buildPerScalaVersionProperty,
    "gradleVersionForBadge" -> gradleVersion.toString.replace("-", "--")
  )
  
  def main(args: Array[String]): Unit =
    def toString(strings: Seq[String]): String = strings.mkString("", "\n", "\n")

    Files.write(File("gradle.properties").getAbsoluteFile, toString(
      Seq(s"${BackendPlugin.scalaBackendProperty} = ${JvmBackend.name}") ++
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
