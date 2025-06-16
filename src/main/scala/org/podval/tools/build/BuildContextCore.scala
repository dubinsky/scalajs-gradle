package org.podval.tools.build

import java.io.File

trait BuildContextCore:
  // Although Gradle caches resolved artifacts and npm caches packages that it retrieves,
  // unpacking frameworks under `/build` after each `./gradlew clean` takes noticeable time (around 14 seconds);
  // so, I am caching unpacked frameworks under `~/.gradle`.
  def frameworks: File
  
  def fatalError(message: String): Nothing
