package org.podval.tools.build

import org.podval.tools.util.Strings

object Dependency:
  final class Repository(
    val url: String,
    val artifactPattern: String,
    val ivy: String
  )
  
trait Dependency:
  override def toString: String = s"$group:$artifact${Strings.prefix("_", backendSuffix)}:$versionDefault"

  def name: String

  def group: String

  def versionDefault: Version

  def artifact: String

  def backendSuffix: Option[String]

  def classifier(version: Version): Option[String]

  def extension (version: Version): Option[String]

  def repository: Option[Dependency.Repository] = None

  final protected def withVersion(
    version: Version.Pre,
    scalaVersion: Option[Version]
  ): DependencyVersion = DependencyVersion(
    dependency = this,
    version = version,
    scalaVersion = scalaVersion
  )
   