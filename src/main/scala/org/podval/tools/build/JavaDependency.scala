package org.podval.tools.build

trait JavaDependency extends NonScalaDependency:
  final override def classifier(version: PreVersion): Option[String] = None
  final override def extension (version: PreVersion): Option[String] = None
 