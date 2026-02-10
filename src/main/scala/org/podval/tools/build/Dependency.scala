package org.podval.tools.build

import org.podval.tools.platform.Strings

trait Dependency:
  override def toString: String = s"$group:$artifact${Strings.prefix("_", backendSuffix)}:$versionDefault"

  def name: String

  def group: String

  def versionDefault: Version

  def artifact: String

  def backendSuffix: Option[String]

  def classifier(version: Version): Option[String]

  def extension (version: Version): Option[String]

  final protected def withVersion(
    version: Version.Pre,
    scalaVersion: Option[Version]
  ): WithVersion = WithVersion(
    dependency = this,
    version = version,
    scalaVersion = scalaVersion
  )
  