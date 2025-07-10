package org.podval.tools.platform

import java.io.File
import scala.sys.process.Process

object Exec:
  def apply(command: String): String = Process(command).!!.trim

  def unameM: String = Exec("uname -m")

  def which(what: String): Option[File] =
    attempt(command = s"which $what").map(File(_))

  private def attempt(command: String): Option[String] =
    try Some(Exec(command)) catch case _: Exception => None
