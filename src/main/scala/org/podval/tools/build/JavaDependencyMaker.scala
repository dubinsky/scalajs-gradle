package org.podval.tools.build

trait JavaDependencyMaker extends SimpleDependencyMaker[JavaDependency]:
  final override def findable: JavaDependency = JavaDependency(this)
  final override def classifier(version: PreVersion): Option[String] = None
  final override def extension(version: PreVersion): Option[String] = None
 