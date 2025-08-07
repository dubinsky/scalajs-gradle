package org.podval.tools.scalajs

import org.podval.tools.build.{ScalaBinaryVersion, Version}
import org.podval.tools.nonjvm.NonJvmBackend.Dep

object ScalaJSDependency:
  object Library        extends Dep("scalajs-library"          , "Library"                           , _.scala2.jvm)
  object CompilerPlugin extends Dep("scalajs-compiler"         , "Compiler Plugin for Scala 2"       , _.scala2    )
  object Junit4Plugin   extends Dep("scalajs-junit-test-plugin", "JUnit4 Compiler Plugin for Scala 2", _.scala2    )
  object Linker         extends Dep("scalajs-linker"           , "Linker"                            , _.scala2    )
  object TestAdapter    extends Dep("scalajs-sbt-test-adapter" , "Test Adapter for Node.js"          , _.scala2    )
  object TestBridge     extends Dep("scalajs-test-bridge"      , "Test Bridge for Node.js"           , _.scala2.jvm)
  
  // There is no Scala 2 equivalent.
  object Scala3Library extends Dep(
    "scala3-library",
    "Scala 3 library in Scala.js",
    _.scala3,
    groupOverride = Some(ScalaBinaryVersion.group),
    versionOverride = Some(ScalaBinaryVersion.Scala3.versionDefault)
  )

  object JavaLogging extends Dep(
    "scalajs-java-logging",
    "Port of the java.util.logging API of JDK 8 for Scala.js",
    _.scala2,
    versionOverride = Some(Version("1.0.0"))
  )

  object Dom extends Dep(
    "scalajs-dom",
    "Library for DOM manipulations",
    versionOverride = Some(Version("2.8.1"))
  )

  object JsDomNode extends Dep(
    "scalajs-env-jsdom-nodejs",
    "Node.js JavaScript environment with JSDOM",
    _.scala2,
    versionOverride = Some(Version("1.1.0"))
  )

  object Playwright extends Dep(
    "scala-js-env-playwright",
    "Playwright JavaScript environment",
    _.scala2.jvm,
    groupOverride = Some("io.github.gmkumar2005"),
    versionOverride = Some(Version("0.1.18"))
  )
