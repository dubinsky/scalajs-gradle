package org.podval.tools.build

sealed trait ScalaBinaryVersion extends JavaDependency derives CanEqual:
  final override def group: String = ScalaBinaryVersion.group
  final override def toString: String = prefix.toString
  def prefix: Version
  final def is(version: Version): Boolean = version.startsWith(prefix)
  final override def versionDefault: Version = scalaVersionDefault.version
  def scalaVersionDefault: ScalaVersion

object ScalaBinaryVersion:
  val group: String = "org.scala-lang"

  def all: Seq[ScalaBinaryVersion] = Seq(Scala3, Scala2_13, Scala2_12)
  
  object Scala3 extends ScalaBinaryVersion:
    override def artifact: String = "scala3-library_3"
    override def description: String = "Scala 3 Library."
    override val prefix: Version = Version("3")
    override val scalaVersionDefault: ScalaVersion = ScalaVersion("3.8.0")
    def scala2VersionDefault: ScalaVersion = Scala2_13.scalaVersionDefault
    private val versionLibraryCompiledWithScala3: Version = Version("3.8.0")
    def libraryCompiledWithScala2(scalaVersion: ScalaVersion): Boolean = versionLibraryCompiledWithScala3.after(scalaVersion.version)

  sealed trait Scala2 extends ScalaBinaryVersion:
    final override def artifact: String = "scala-library"
    final override def description: String = "Scala 2 Library."

  object Scala2_13 extends Scala2:
    override val prefix: Version = Version("2.13")
    override val scalaVersionDefault: ScalaVersion = ScalaVersion("2.13.18")

  object Scala2_12 extends Scala2:
    override val prefix: Version = Version("2.12")
    override val scalaVersionDefault: ScalaVersion = ScalaVersion("2.12.21")
