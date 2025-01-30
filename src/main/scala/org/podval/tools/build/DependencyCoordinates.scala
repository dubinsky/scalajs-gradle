package org.podval.tools.build

// TODO two inheritors?
trait DependencyCoordinates:
  def group: String
  def artifact: String
  def classifier(version: Version): Option[String]
  def extension(version: Version): Option[String]
