package org.podval.tools.build

trait DependencyCoordinates:
  def group: String
  def artifact: String
  def classifier(version: PreVersion): Option[String]
  def extension(version: PreVersion): Option[String]
