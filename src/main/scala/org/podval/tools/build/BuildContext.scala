package org.podval.tools.build

import java.io.File

trait BuildContext extends BuildContextCore:
  def getArtifact(repository: Option[Repository], dependencyNotation: String): Option[File]

  def unpackArchive(file: File, isZip: Boolean, into: File): Unit
