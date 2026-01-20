package org.podval.tools.build

sealed trait ScalaBinaryVersion extends JavaDependency derives CanEqual:
  final override def group: String = ScalaBinaryVersion.group

  final override def toString: String = prefix.toString

  def prefix: Version

  final def is(version: Version): Boolean = version.startsWith(prefix) && isInRange(version)

  def isInRange(version: Version): Boolean

  final override def versionDefault: Version = scalaVersionDefault.version

  def scalaVersionDefault: ScalaVersion.Known

object ScalaBinaryVersion:
  val group: String = "org.scala-lang"

  def all: Seq[ScalaBinaryVersion] = Seq(
    Scala3WithScala3Library,
    Scala3WithScala2Library,
    Scala2_13,
    Scala2_12
  )

  sealed trait Scala3 extends ScalaBinaryVersion:
    final override def artifact: String = "scala3-library_3"
    final override def description: String = "Scala 3 Library."
    final override val prefix: Version = Version("3")

  private val versionScala3LibraryCompiledWithScala3: Version = Version("3.8.0")

  case object Scala3WithScala3Library extends Scala3:
    override def isInRange(version: Version): Boolean = version >= versionScala3LibraryCompiledWithScala3
    override val scalaVersionDefault: ScalaVersion.Known = ScalaVersion("3.8.1")

  case object Scala3WithScala2Library extends Scala3:
    override def isInRange(version: Version): Boolean = version < versionScala3LibraryCompiledWithScala3
    override val scalaVersionDefault: ScalaVersion.Known = ScalaVersion("3.7.4")

  sealed trait Scala2 extends ScalaBinaryVersion:
    final override def artifact: String = "scala-library"
    final override def description: String = "Scala 2 Library."
    final override def isInRange(version: Version): Boolean = true

  case object Scala2_13 extends Scala2:
    override val prefix: Version = Version("2.13")
    override val scalaVersionDefault: ScalaVersion.Known = ScalaVersion("2.13.18")

  case object Scala2_12 extends Scala2:
    override val prefix: Version = Version("2.12")
    override val scalaVersionDefault: ScalaVersion.Known = ScalaVersion("2.12.21")
