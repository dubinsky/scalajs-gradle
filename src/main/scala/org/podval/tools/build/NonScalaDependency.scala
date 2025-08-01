package org.podval.tools.build

import org.podval.tools.jvm.JvmBackend

abstract class NonScalaDependency extends Dependency.WithScalaVersion with Dependency:
  final override def dependency: Dependency = this

  final override def scalaBackend: ScalaBackend = JvmBackend
  final override def isBackendSupported(backend: ScalaBackend): Boolean = backend.isJvm
  final override def withBackend(backend: ScalaBackend): this.type =
    require(isBackendSupported(backend))
    this

  final override def artifactNameSuffix: String = ""
  final override def withScalaVersion(scalaLibrary: ScalaLibrary): this.type = this

  final override def forArtifact(artifactName: String): Option[this.type] =
    if artifactName != artifact then None else Some(this)
