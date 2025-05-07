package org.podval.tools.platform

import java.io.File
import org.slf4j.{Logger, LoggerFactory}
import scala.sys.process.{Process, ProcessLogger}

object Exec:
  private val logger: Logger = LoggerFactory.getLogger(Exec.getClass)

  def which(what: String): Option[File] =
    attempt(command = s"which $what").map(File(_))

  def unameM: String = Exec("uname -m")

  private def attempt(command: String): Option[String] =
    try Some(Exec(command)) catch case _: Exception => None

  private def apply(command: String): String = Process(command).!!.trim

  def apply(command: Seq[String]): Unit =
    logger.info("Running " + command.mkString(" "))
    val exitCode: Int = Process(command).!
    logger.info(if exitCode == 0 then "Done" else s"Failed: $exitCode")

  def apply(
    command: File,
    args: String,
    cwd: Option[File],
    extraEnv: (String, String)*
  ): String =
    val cmd: String = s"${command.getAbsolutePath} $args"
    val what: String =
      s"""Exec(
         |  cmd = $cmd,
         |  cwd = $cwd,
         |  extraEnv = $extraEnv
         |)""".stripMargin

    logger.debug(what)

    var err: Seq[String] = Seq.empty
    var out: Seq[String] = Seq.empty

    val exitCode: Int = Process(
      command = cmd,
      cwd,
      extraEnv = extraEnv*
    ).!(ProcessLogger(
      fout = line => out = out :+ line,
      ferr = line => err = err :+ line
    ))

    val errStr: String = err.mkString("\n")
    val outStr: String = out.mkString("\n")

    val result: String = s"$what => exitCode=$exitCode; err=[$errStr]; out=[$outStr]"
    if exitCode == 0
    then logger.debug(result)
    else throw IllegalArgumentException(result)
    outStr
