package org.podval.tools.scalajs

// TODO move into org.opentorah.util
object Colours:
  private val escape: String = "\u001b"
  private val reset: Int = 0

  // TODO make an enum
  val black: Int = 30
  val red: Int = 31
  val green: Int = 32
  val yellow: Int = 33
  val blue: Int = 34
  val magenta: Int = 35
  val cyan: Int = 36
  val white: Int = 37

  def withColour(colour: Int, string: String): String = s"$escape[${colour}m$string$escape[${reset}m"

  // TODO bright colours: add ;1 after the code before 'm'
