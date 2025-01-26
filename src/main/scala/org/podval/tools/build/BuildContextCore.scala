package org.podval.tools.build

import org.slf4j.Logger
import java.io.File

// Integration with the build tool or other execution context.
trait BuildContextCore[L <: Logger]:
  // Frameworks
  // Although Gradle caches resolved artifacts and npm caches packages that it retrieves,
  // unpacking frameworks under `/build` after each `./gradlew clean` takes noticeable time (around 14 seconds);
  // so, I am caching unpacked frameworks under `~/.gradle`.
  def frameworks: File
  
  // Logging
  def logger: L
  
  final def warn(message: String): Unit = logger.warn(message)

  final def error(message: String): Unit = logger.error(message)
  
  final def info(message: String): Unit = logger.info(message)

  def lifecycle(message: String): Unit

  def fatalError(message: String): Nothing
