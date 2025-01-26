package org.podval.tools.build

import org.slf4j.Logger
import java.io.File

trait BuildContext[L <: Logger] extends BuildContextCore[L]:
  def getArtifact(repository: Option[Repository], dependencyNotation: String): Option[File]

  def unpackArchive(file: File, isZip: Boolean, into: File): Unit

  def javaexec(mainClass: String, args: String*): Unit
