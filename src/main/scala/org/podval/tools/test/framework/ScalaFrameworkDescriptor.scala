package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackend, ScalaDependencyMaker, Version}

abstract class ScalaFrameworkDescriptor(
  final override val group: String,
  final override val artifact: String,
  final override val versionDefault: Version,
  final override val description: String,
  final override val name: String,
  final override val className: String,
  final override val sharedPackages: List[String],
  final override val tagOptions: Option[TagOptions],
  final override val usesTestSelectorAsNestedTestSelector: Boolean = false
) extends FrameworkDescriptor with ScalaDependencyMaker:
  final override def forBackend(backend: ScalaBackend): Option[ScalaDependencyMaker] = Some(this.withBackend(backend))
