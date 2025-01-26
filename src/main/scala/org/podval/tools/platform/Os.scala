package org.podval.tools.platform

trait Os derives CanEqual:
  def hasUname: Boolean = false
  def libraryExtension: String = "so"
  def is(name: String): Boolean

object Os:
  private def isLinux(name: String): Boolean = name.startsWith("Lin")
  
  private def isAndroid: Boolean = System.getProperty("java.specification.vendor").contains("Android")
  
  case object Android extends Os:
    override def is(name: String): Boolean = isLinux(name) && isAndroid

  case object Linux extends Os:
    override def is(name: String): Boolean = isLinux(name) && !isAndroid
    override def hasUname: Boolean = true

  case object Mac extends Os:
    override def is(name: String): Boolean = name.startsWith("Mac")
    override def libraryExtension: String = "dylib"

  case object Windows extends Os:
    override def is(name: String): Boolean = name.startsWith("Win")
    override def libraryExtension: String = "dll"

  case object Aix extends Os:
    override def is(name: String): Boolean = name.startsWith("Aix")

  case object FreeBSD extends Os:
    override def is(name: String): Boolean = name.startsWith("Free")

  case object SunOS extends Os:
    override def is(name: String): Boolean = name.startsWith("Sun")

  val values: Seq[Os] = Seq(Android, Linux, Mac, Windows, Aix, FreeBSD, SunOS)

  def getName: String = System.getProperty("os.name")

  def get: Os = get(getName)

  def get(name: String): Os = values
    .find(_.is(name))
    .getOrElse(throw IllegalArgumentException(s"Unsupported Operating System: $name"))
