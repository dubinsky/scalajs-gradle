package org.podval.tools.test.framework

import org.podval.tools.build.{Backend, ScalaDependency, Version}
import org.podval.tools.jvm.JvmBackend

abstract class ScalaFramework(
  name: String,
  group: String,
  versionDefault: Version,
  artifact: String,
  nameSbt: String,
  className: String,
  sharedPackages: List[String],
  tagOptions: Option[TagOptions] = None,
  usesTestSelectorAsNested: Boolean = false,
  additionalOptions: Array[String] = Array.empty
) extends Framework(
  name = name,
  nameSbt = nameSbt,
  className = className,
  sharedPackages = sharedPackages,
  tagOptions = tagOptions,
  usesTestSelectorAsNested = usesTestSelectorAsNested,
  additionalOptions = additionalOptions
):
  final override def dependency: ScalaDependency = ScalaDependency(
    backend = JvmBackend,
    name = name,
    group = group,
    versionDefault = versionDefault,
    artifact = artifact
  )
