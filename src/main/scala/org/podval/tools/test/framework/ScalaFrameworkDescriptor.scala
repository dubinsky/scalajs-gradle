package org.podval.tools.test.framework

import org.podval.tools.build.{DependencyMaker, ScalaBackend, ScalaBinaryVersion, ScalaDependencyMaker, ScalaVersion,
  Version}

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
) extends FrameworkDescriptor with ScalaDependencyMaker with DependencyMaker.Jvm:
  override def maker(backend: ScalaBackend): Option[ScalaDependencyMaker] = Some:
    val delegate: ScalaFrameworkDescriptor = ScalaFrameworkDescriptor.this
    new ScalaDependencyMaker:
      override def scalaBackend: ScalaBackend = backend
      override def group: String = delegate.group
      override def artifact: String = delegate.artifact
      override def versionDefault: Version = delegate.versionDefault
      override def versionDefaultFor(backend: ScalaBackend, scalaVersion: ScalaVersion): Version = delegate.versionDefaultFor(backend, scalaVersion)
      override def description: String = delegate.description
      override def isVersionCompound: Boolean = delegate.isVersionCompound
      override def isDependencyRequirementVersionExact: Boolean = delegate.isDependencyRequirementVersionExact
      override def isPublishedFor(binaryVersion: ScalaBinaryVersion): Boolean = delegate.isPublishedFor(binaryVersion)
      override def isScalaVersionFull: Boolean = delegate.isScalaVersionFull
