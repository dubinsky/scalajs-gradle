package org.podval.tools.platform

enum Architecture derives CanEqual:
  case i686
  case x86_64
  case amd64
  case ppc64
  case ppc64le
  case s390x
  case nacl
  case aarch64
  case armv6l
  case armv7l
  case armv8l

object Architecture:
  // Gradle Node plugin's code claims that Java returns "arm" on all ARM variants.
  private def getNameFromEnvironment: String = System.getProperty("os.arch")

  private def getNameFromSystem: String = Exec.unameM

  def getName: String = if Os.get.hasUname then getNameFromSystem else getNameFromEnvironment

  def get: Architecture =
    val name: String = getName
    Architecture
      .values
      .find(_.toString.toLowerCase == name.toLowerCase)
      .getOrElse(throw IllegalArgumentException(s"Unsupported architecture: $name"))
