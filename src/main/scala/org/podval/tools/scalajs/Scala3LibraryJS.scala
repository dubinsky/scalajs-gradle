package org.podval.tools.scalajs

import org.podval.tools.build.{ScalaBinaryVersion, ScalaDependencyMaker, Version}

// There is no Scala 2 equivalent
object Scala3LibraryJS extends ScalaDependencyMaker:
  override def versionDefault: Version = ScalaBinaryVersion.Scala3.versionDefault.version
  override def group: String = ScalaBinaryVersion.group
  override def artifact: String = "scala3-library"
  override def description: String = "Scala 3 library in Scala.js."
  override def scalaBackend: ScalaJSBackend.type = ScalaJSBackend
