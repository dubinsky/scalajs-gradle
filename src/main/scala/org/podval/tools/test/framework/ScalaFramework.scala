package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaDependency, Version}
import org.podval.tools.jvm.JvmBackend

abstract class ScalaFramework(
  final override val group: String,
  final override val artifact: String,
  final override val versionDefault: Version,
  final override val description: String,
  final override val name: String,
  final override val className: String,
  final override val sharedPackages: List[String],
  final override val tagOptions: Option[TagOptions] = None,
  final override val usesTestSelectorAsNested: Boolean = false
) extends Framework with ScalaDependency:
  override def scalaBackend: JvmBackend.type = JvmBackend
