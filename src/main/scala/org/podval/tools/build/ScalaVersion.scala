package org.podval.tools.build

sealed trait ScalaVersion derives CanEqual:
  def binaryVersion: ScalaBinaryVersion
  def version: Version

object ScalaVersion:
  final case class Known(
    override val binaryVersion: ScalaBinaryVersion,
    override val version: Version
  ) extends ScalaVersion:
    require(binaryVersion.is(version))

    override def toString: String = version.toString

    override def equals(other: Any): Boolean = other.asInstanceOf[Matchable] match
      case that: ScalaVersion => this.version == that.version
      case _ => false

  def apply(string: String): Known = apply(Version(string))

  def apply(version: Version): Known = Known(
    version = version,
    binaryVersion = ScalaBinaryVersion
      .all
      .find(_.is(version))
      .getOrElse(throw IllegalArgumentException(s"Unrecognized Scala version $version"))
  )

  // ScalaLibrary.fromScalaVersion() for Scala 3 before Scala 3.8.0,
  // we know that Scala 2 version is 2.13.?, but we do not know the full version -
  // and we do not need it, since the full Scala 2 version
  // matters only for dependencies with `isScalaVersionFull`,
  // ScalaLibrary.fromScalaVersion() is only used with `FrameworkDescriptor` dependencies,
  // which are not `isScalaVersionFull` (only Scala compiler plugins are).
  case object Unknow2_13 extends ScalaVersion:
    override def binaryVersion: ScalaBinaryVersion = ScalaBinaryVersion.Scala2_13

    override def toString: String = s"Unknown $binaryVersion Scala version"

    override def equals(other: Any): Boolean = other.asInstanceOf[Matchable] match
      case that: AnyRef => that.eq(this)
      case _ => false

    override def version: Version = throw IllegalArgumentException(
      s"""$this: full version is not know;
         |ScalaLibrary was constructed from a Scala 3 version,
         |not from a classpath nor Gradle configuration
         |""".stripMargin
    )
