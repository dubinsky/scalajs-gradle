package org.podval.tools.test.testproject

import org.podval.tools.platform.Files

final class SourceFile(val name: String, val content: String)

object SourceFile:
  def fromResource(name: String): SourceFile = SourceFile(
    name,
    content = Files.read(Files.url2file(getClass.getResource(s"$name.scala"))).mkString("\n")
  )
