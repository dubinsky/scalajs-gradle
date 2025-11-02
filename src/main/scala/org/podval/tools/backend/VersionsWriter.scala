package org.podval.tools.backend

import org.podval.tools.build.{ScalaBackend, ScalaBinaryVersion, Version}
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.node.NodeDependency
import org.podval.tools.platform.{Files, Strings}
import org.podval.tools.scalajs.ScalaJSBackend
import org.podval.tools.scalanative.ScalaNativeBackend
import org.podval.tools.test.framework
import java.io.File

// This writes versions of everything into an AsciiDoc file that the documentation uses;
// this way, the versions are guaranteed to be consistent - if this was run ;)
object VersionsWriter:
  private val gradleVersion: Version = Version("9.2.1")

  private val versions: Seq[(String, Version)] = Seq(
    "plugin" -> Version("0.9.15"),

    "gradle" -> gradleVersion,
    
    "scala"     -> ScalaBinaryVersion.Scala3   .scalaVersionDefault.version,
    "scala-213" -> ScalaBinaryVersion.Scala2_13.scalaVersionDefault.version,
    "scala-212" -> ScalaBinaryVersion.Scala2_12.scalaVersionDefault.version,

    "sbt-test-interface"           -> JvmBackend.sbtTestInterfaceVersion,

    "scalanative"                  -> ScalaNativeBackend.versionDefault,

    "scalajs"                      -> ScalaJSBackend    .versionDefault,

    "scalajs-dom"                  -> ScalaJSBackend.domVersion,
    "scalajs-env-jsdom-nodejs"     -> ScalaJSBackend.jsDomNodeVersion,
    "scala-js-env-playwright"      -> ScalaJSBackend.playwrightVersion,

    "node"                         -> NodeDependency.dependency     .versionDefault,

    "junit"                        -> framework.JUnit4Jvm.Underlying.versionDefault,
    "framework-junit4-jvm"         -> framework.JUnit4Jvm           .versionDefault,
    "framework-junit4-scalajs"     -> framework.JUnit4ScalaJS       .versionDefault,
    "framework-junit4-scalanative" -> framework.JUnit4ScalaNative   .versionDefault,
    "framework-airspec"            -> framework.AirSpec             .versionDefault,
    "framework-hedgehog"           -> framework.Hedgehog            .versionDefault,
    "framework-munit"              -> framework.MUnit               .versionDefault,
    "framework-scalacheck"         -> framework.ScalaCheck          .versionDefault,
    "framework-scalaprops"         -> framework.Scalaprops          .versionDefault,
    "framework-scalatest"          -> framework.ScalaTest           .versionDefault,
    "framework-specs2"             -> framework.Specs2              .versionDefault,
    "framework-specs2-scala2"      -> framework.Specs2              .versionDefaultScala2,
    "framework-utest"              -> framework.UTest               .versionDefault,
    "framework-weaver"             -> framework.WeaverTest          .versionDefault,
    "framework-zio-test"           -> framework.ZioTest             .versionDefault
  )

  private val attributes: Seq[(String, String)] = Seq(
    "pluginScalaBackendProperty"  -> ScalaBackend.property,
    "gradleVersionForBadge" -> gradleVersion.toString.replace("-", "--")
  )
  
  def main(args: Array[String]): Unit =
    def toString(strings: Seq[String]): String = strings.mkString("", "\n", "\n")

    Files.write(File("gradle.properties").getAbsoluteFile, toString(
      Seq(s"${ScalaBackend.property} = ${JvmBackend.name}") ++
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
