package org.podval.tools.build

import org.podval.tools.jvm.JvmBackend

abstract class NonScalaDependency extends Dependency.WithScalaVersion with Dependency:
  final override def isJvm: Boolean = true
  final override def scalaBackend: ScalaBackend = JvmBackend
  final override def isBackendSupported(backend: ScalaBackend): Boolean = backend.isJvm
  final override def dependency: this.type = this
  final override def withScalaVersion(scalaLibrary: ScalaLibrary): this.type = this
  final override def artifactNameSuffix(backendOverride: Option[ScalaBackend]): String = ""
  final override def forArtifact(artifactName: String): Option[this.type] = Option.when(artifactName == artifact)(this)
