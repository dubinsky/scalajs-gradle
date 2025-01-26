package org.podval.tools.build

import org.podval.tools.util.Logging
import org.slf4j.Logger
import java.io.File

final class DefaultBuildContext(
  override val logger: Logger,
  override val frameworks: File = File(Option(System.getenv("HOME")).map(_ + "/.gradle").getOrElse("/tmp"))
) extends BuildContext[Logger]:
  Logging.setWarn(logger)
  
  override def lifecycle(message: String): Unit = logger.warn(message)

  override def fatalError(message: String): Nothing = throw IllegalArgumentException(s"Fatal error in $this: $message")

  override def getArtifact(repository: Option[Repository], dependencyNotation: String): Option[File] = None

  override def unpackArchive(file: File, isZip: Boolean, into: File): Unit =
    throw UnsupportedOperationException("unpackArchive() is not available")

  override def javaexec(mainClass: String, args: String*): Unit = Class
    .forName(mainClass)
    .getMethod("main", classOf[Array[String]])
    .invoke(null, args.toArray)
