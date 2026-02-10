package org.podval.tools.build

sealed abstract class ScalaBinaryVersion(
  name: String,
  artifact: String,
  scalaVersion: Option[Version],
  val prefix: Version,
  versionDefault: Version
) extends JavaDependency(
  name = name,
  group = ScalaBinaryVersion.group,
  versionDefault = versionDefault,
  artifact = artifact,
  scalaVersion = scalaVersion
) derives CanEqual:
  final val scalaVersionDefault: ScalaVersion.Known = ScalaVersion.Known(this, versionDefault)

  final def is(version: Version): Boolean = version.startsWith(prefix) && isInRange(version)

  def isInRange(version: Version): Boolean

object ScalaBinaryVersion:
  val group: String = "org.scala-lang"

  def all: Seq[ScalaBinaryVersion] = Seq(
    Scala3WithScala3Library,
    Scala3WithScala2Library,
    Scala2_13,
    Scala2_12
  )

  sealed abstract class Scala3(
    versionDefault: Version
  ) extends ScalaBinaryVersion(
    artifact = "scala3-library",
    scalaVersion = Some(Version("3")),
    name = "Scala 3 Library.",
    prefix = Version("3"),
    versionDefault = versionDefault
  )

  private val versionScala3LibraryCompiledWithScala3: Version = Version("3.8.0")

  case object Scala3WithScala3Library extends Scala3(
    versionDefault = Version("3.8.2")
  ):
    override def isInRange(version: Version): Boolean = version >= versionScala3LibraryCompiledWithScala3

  case object Scala3WithScala2Library extends Scala3(
    versionDefault = Version("3.7.4")
  ):
    override def isInRange(version: Version): Boolean = version < versionScala3LibraryCompiledWithScala3

  sealed abstract class Scala2(
    prefix: Version,
    versionDefault: Version
  ) extends ScalaBinaryVersion(
    artifact = "scala-library",
    scalaVersion = None,
    name = "Scala 2 Library.",
    prefix = prefix,
    versionDefault = versionDefault
  ):
    final override def isInRange(version: Version): Boolean = true

  case object Scala2_13 extends Scala2(
    prefix = Version("2.13"),
    versionDefault = Version("2.13.18")
  )

  case object Scala2_12 extends Scala2(
    prefix = Version("2.12"),
    versionDefault = Version("2.12.21")
  )
