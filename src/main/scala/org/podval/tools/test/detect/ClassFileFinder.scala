package org.podval.tools.test.detect

import org.gradle.api.internal.tasks.testing.detection.ClassFileExtractionManager
import org.gradle.internal.Factory
import org.slf4j.{Logger, LoggerFactory}
import java.io.File

object ClassFileFinder:
  private val logger: Logger = LoggerFactory.getLogger(classOf[ClassFileFinder])

final class ClassFileFinder(
  testClasspath: List[File],
  testClassesDirectories: List[File],
  testTaskTemporaryDir: Factory[File]
):
  ClassFileFinder.logger.debug(s"ClassFileFinder(testClasspath=$testClasspath, testClassesDirectories=$testClassesDirectories)")

  private val testClassDirectories: List[File] = testClassesDirectories ++ testClasspath.filter(_.isDirectory)

  private val classFileExtractionManager: ClassFileExtractionManager = ClassFileExtractionManager(testTaskTemporaryDir)

  testClasspath
    .filter((file: File) => file.isFile && file.getPath.endsWith(".jar"))
    .foreach((libraryJar: File) =>
      ClassFileFinder.logger.debug(s"SbtTestFrameworkDetector: libraryJar $libraryJar")
      classFileExtractionManager.addLibraryJar(libraryJar)
    )

  def findClassFile(typeName: String): Option[File] = testClassDirectories
    .map((testClassDirectory: File) => File(testClassDirectory, s"$typeName.class"))
    .find(_.exists)
    .orElse(Option(classFileExtractionManager.getLibraryClassFile(typeName)))
    .orElse:
      ClassFileFinder.logger.info(s"SbtTestFrameworkDetector: could not find $typeName file to scan.")
      None
