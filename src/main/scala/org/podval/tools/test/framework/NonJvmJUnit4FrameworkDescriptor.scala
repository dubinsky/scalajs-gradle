package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackend, ScalaDependencyMaker, Version}
import org.podval.tools.nonjvm.NonJvmBackend

abstract class NonJvmJUnit4FrameworkDescriptor(
  final override val artifact: String,
  final override val name: String,
  final override val className: String,
  final override val sharedPackages: List[String],
) extends FrameworkDescriptor with ScalaDependencyMaker:
  def supportedBackend: NonJvmBackend

  final override def group: String = supportedBackend.group
  final override def versionDefault: Version = supportedBackend.versionDefault
  final override def description: String = s"JUnit4 for ${supportedBackend.name}"
  final override def tagOptions: Option[TagOptions] = None
  final override def usesTestSelectorAsNestedTestSelector: Boolean = JUnit4.usesTestSelectorAsNestedTestSelector
  //final override def additionalOptions(isRunningInIntelliJ: Boolean): Array[String] = Array.empty

  final override def forBackend(backend: ScalaBackend): Option[ScalaDependencyMaker] =
    if backend != supportedBackend then None else Some(this)
