package org.podval.tools.test.framework

import org.podval.tools.build.{Backend, ScalaDependency, ScalaTestFramework, Version}
import org.podval.tools.scalajs.ScalaJSBackend
import org.podval.tools.scalanative.ScalaNativeBackend

object WeaverTest extends ScalaTestFramework(
  name = "Weaver-test",
  nameSbt = "weaver-cats-effect",
  group = "org.typelevel",
  artifact = "weaver-cats",
  versionDefault = Version("0.11.3"),
  className = "weaver.framework.CatsEffect",
  sharedPackages = List("weaver")
):
  override def isBackendSupported(backend: Backend): Boolean = backend match
    case ScalaNativeBackend => false
    case ScalaJSBackend     => false
    case _                  => true
 