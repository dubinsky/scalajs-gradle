package org.podval.tools.build

import org.podval.tools.backend.scalajs.ScalaJSBackend

sealed trait ScalaBinaryVersion derives CanEqual:
  def versionMajor: Int
  def versionSuffixLength: Int
  def versionDefault: ScalaVersion
  def artifact: String
  def description: String
  
  final def versionSuffix: Version.Simple = versionDefault.version.take(versionSuffixLength)

  final def scalaLibraryDependency: JavaDependency = scalaLibrary.dependency

  private def scalaLibrary: JavaDependency.Maker = new JavaDependency.Maker:
    override def group: String = ScalaBinaryVersion.group
    override def versionDefault: Version.Simple = ScalaBinaryVersion.this.versionDefault.version
    override def artifact: String = ScalaBinaryVersion.this.artifact
    override def description: String = ScalaBinaryVersion.this.description

object ScalaBinaryVersion:
  val group: String = "org.scala-lang"

  def forScalaVersion(scalaVersion: ScalaVersion): ScalaBinaryVersion =
    if scalaVersion.version.major == Scala3.versionMajor
    then Scala3
    else
      if scalaVersion.version.minor == Scala213.versionMinor
      then 
        Scala213
      else
        require(scalaVersion.version.minor == Scala212.versionMinor)
        Scala212

  def versionDefaults: Seq[ScalaVersion] = Seq(
    Scala3  .versionDefault,
    Scala213.versionDefault,
    Scala212.versionDefault
  )
  
  object Scala3 extends ScalaBinaryVersion:
    override def versionMajor: Int = 3
    override def versionSuffixLength: Int = 1
    override def artifact: String = "scala3-library_3"
    override def description: String =  "Scala 3 Library."
    override def versionDefault: ScalaVersion = ScalaVersion("3.7.1")

    // There is no Scala 2 equivalent
    object ScalaLibraryJS extends ScalaDependency.Maker:
      override def versionDefault: Version.Simple = Scala3.versionDefault.version
      override def group: String = ScalaBinaryVersion.group
      override def artifact: String = "scala3-library"
      override def description: String = "Scala 3 library in Scala.js."
      override def scalaBackend: ScalaJSBackend.type = ScalaJSBackend

  sealed trait Scala2 extends ScalaBinaryVersion:
    final override def versionMajor: Int = 2
    final override def versionSuffixLength: Int = 2
    override def artifact: String = "scala-library"
    override def description: String = "Scala 2 Library."
    def versionMinor: Int
  
  object Scala213 extends Scala2:
    override def versionMinor: Int = 13
    val versionDefault: ScalaVersion = ScalaVersion("2.13.16")

  object Scala212 extends Scala2:
    override def versionMinor: Int = 12
    val versionDefault: ScalaVersion = ScalaVersion("2.12.20")
