package org.podval.tools.build

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
  trait Maker extends Dependency.Maker[Any]:
    final override def findable(platform: Any): JavaDependency = JavaDependency(
      group, 
      artifact
    )

    final override def dependency(platform: Any): JavaDependency = findable(platform)
    