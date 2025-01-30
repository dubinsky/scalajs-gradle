package org.podval.tools.build

final class JavaDependency(
  group: String,
  artifact: String
) extends SimpleDependency[JavaDependency](
  group = group,
  artifact = artifact
):
  override def classifier(version: Version): Option[String] = None
  override def extension (version: Version): Option[String] = None

  def required(
    version: Version,
    reason: String,
    configurationName: String,
    isVersionExact: Boolean = false
  ): DependencyRequirement = DependencyRequirement(
    findable = this,
    dependency = this,
    version,
    reason,
    configurationName,
    isVersionExact
  )
