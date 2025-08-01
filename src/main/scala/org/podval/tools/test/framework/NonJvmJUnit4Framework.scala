package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackend, ScalaDependency, Version}
import org.podval.tools.nonjvm.NonJvmBackend

abstract class NonJvmJUnit4Framework(
  final override val artifact: String,
  final override val name: String,
  final override val className: String,
  final override val sharedPackages: List[String]
) extends Framework with ScalaDependency:
  def supportedBackend: NonJvmBackend

  final override def isBackendSupported(backend: ScalaBackend): Boolean = backend == supportedBackend
  final override def group: String = supportedBackend.group
  final override def versionDefault: Version = supportedBackend.versionDefault
  final override def description: String = s"JUnit4 for ${supportedBackend.name}"
  final override def tagOptions: Option[TagOptions] = None
  final override def usesTestSelectorAsNested: Boolean = JUnit4Jvm.usesTestSelectorAsNested
