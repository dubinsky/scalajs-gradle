package org.podval.tools.build

trait JavaDependency extends NonScalaDependency:
  final override def classifier(version: Version): Option[String] = None
  final override def extension (version: Version): Option[String] = None
 