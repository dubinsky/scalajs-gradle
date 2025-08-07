package org.podval.tools.platform

import org.slf4j.{Logger, LoggerFactory}
import scala.io.Source
import java.io.{BufferedWriter, File, FileWriter}
import java.net.URL
import java.nio.file.Paths

object Files:
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def write(file: File, content: String): Unit =
    logger.debug(s"Writing $file")
    file.getParentFile.mkdirs()
    val writer: BufferedWriter = BufferedWriter(new FileWriter(file))
    try writer.write(content) finally writer.close()

  def read(file: File): Seq[String] =
    val source = Source.fromFile(file)
    // `toList` materializes the iterator before closing the source
    val result = source.getLines().toList
    source.close
    result

  def readBytes(file: File): Array[Byte] =
    java.nio.file.Files.readAllBytes(Paths.get(file.toURI))

  def writeBytes(file: File, content: Array[Byte]): Unit =
    java.nio.file.Files.write(Paths.get(file.toURI), content)

  def url2file(url: URL): File = Paths.get(url.toURI).toFile

  def file(directory: File, segments: String*): File = fileSeq(directory, segments)

  @scala.annotation.tailrec
  def fileSeq(directory: File, segments: Seq[String]): File =
    if segments.isEmpty then directory
    else fileSeq(File(directory, segments.head), segments.tail)

  def listDirectories(directory: File): Seq[File] = Option(directory.listFiles)
    .map(_.toSeq.filter(_.isDirectory))
    .getOrElse(Seq.empty)
  