package org.podval.tools.build

import org.podval.tools.build.jvm.JvmBackend

final class JavaDependency private(
  group: String,
  artifact: String
) extends SimpleDependency[JavaDependency](
  group = group,
  artifact = artifact
):
  override def classifier(version: Version): Option[String] = None
  override def extension (version: Version): Option[String] = None

object JavaDependency:
  trait Maker extends Dependency.Maker:
    final override def scalaBackend: JvmBackend.type = JvmBackend
    final override def findable: JavaDependency = JavaDependency(group, artifact)
    final override def dependency(scalaVersion: ScalaVersion): JavaDependency = findable
    final def dependency: JavaDependency = findable
