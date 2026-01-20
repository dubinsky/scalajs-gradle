package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackend, ScalaDependency, Version}
import org.podval.tools.scalajs.ScalaJSBackend
import org.podval.tools.scalanative.ScalaNativeBackend

object WeaverTest extends ScalaFramework(
  name = "weaver-cats-effect",
  description = "Weaver-test",
  group = "org.typelevel",
  artifact = "weaver-cats",
  versionDefault = Version("0.11.3"),
  className = "weaver.framework.CatsEffect",
  sharedPackages = List("weaver")
):
  override def isBackendSupported(backend: ScalaBackend): Boolean = backend match
    case ScalaNativeBackend => false
    case ScalaJSBackend     => false
    case _                  => true
 