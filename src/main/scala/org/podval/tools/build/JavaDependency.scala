package org.podval.tools.build

open class JavaDependency(
  group: String,
  artifact: String
) extends Dependency.Simple(
  group = group,
  artifact = artifact
):
  final override def classifier(version: Version): Option[String] = None

  final override def extension(version: Version): Option[String] = None

object JavaDependency:
  open class Requirement(
    dependency: JavaDependency,
    version: Version,
    reason: String,
    configurations: Configurations,
    isVersionExact: Boolean = false
  ) extends DependencyRequirement(
    dependency,
    version,
    reason,
    configurations,
    isVersionExact
  ):
    override protected def getDependency: Dependency = dependency
