package org.podval.tools.util

import org.gradle.api.Project
import org.gradle.api.file.FileTree
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

  def write(
    file: File,
    content: Seq[String]
  ): Unit = write(
    file.getAbsoluteFile, 
    Strings.toString(content)
  )
    
  def splice(
    file: File,
    boundary: String,
    patch: Seq[String]
  ): Unit = write(
    file,
    Strings.splice(
      in = read(file.getAbsoluteFile),
      boundary = boundary,
      patch = patch
    )
  )

  def unpack(
    project: Project,
    artifact: File,
    into: File
  ): Unit =
    val isZip: Boolean = artifact.getName.endsWith(".zip")
    val from: FileTree = (if isZip then project.zipTree else project.tarTree)(artifact)
    into.mkdir()
    project.copy(_.from(from).into(into): Unit)
