package org.podval.tools.backend

import org.podval.tools.build.Backend
import org.podval.tools.util.{Files, Strings}
import java.io.File

// Directory names that select a backend or a shared source set.
// `js`, `jvm`, `native`, `shared`, and partial shares such as `js-native` or `shared-jvm-js`.
object BackendLayout:
  // Direct child directories of `directory` that this plugin treats as projects.
  def projectDirectories(directory: File): Seq[String] = Files
    .listDirectories(directory)
    .map(_.getName)
    .filter(isBackendDirectory)
    .sorted
  
  def isBackendDirectory(name: String): Boolean =
    backend(name).isDefined || sharedBackends(name).isDefined

  def backend(sourceRoot: String): Option[Backend] = Backend
    .all
    .find(_.sourceRoot == sourceRoot)

  private val sharedSourceRoot: String = "shared"

  // Backends that share this directory, when the name is a valid share.
  // `shared` is every backend. A single backend, all backends spelled out, and duplicates are rejected.
  def sharedBackends(sourceRoot: String): Option[Set[Backend]] =
    if sourceRoot == sharedSourceRoot then Some(Backend.all) else
      val names: Seq[String] = Strings.dropPrefixIfPresent(sourceRoot, s"$sharedSourceRoot-").split("-", -1).toSeq
      val backends: Seq[Backend] = names.flatMap(backend)
      Option.when(
        names.nonEmpty &&
        backends.length == names.length &&
        backends.distinct.length == names.length &&
        backends.length > 1 &&
        backends.length < Backend.all.size
      )(backends.toSet)
