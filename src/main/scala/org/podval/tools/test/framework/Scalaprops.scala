package org.podval.tools.test.framework

import org.podval.tools.build.{Backend, ScalaTestFramework, Version}
import org.podval.tools.scalajs.ScalaJSBackend
import org.podval.tools.scalanative.ScalaNativeBackend

object Scalaprops extends ScalaTestFramework(
  name = "Scalaprops",
  nameSbt = "Scalaprops",
  group = "com.github.scalaprops",
  artifact = "scalaprops",
  versionDefault = Version("0.10.0"),
  className = "scalaprops.ScalapropsFramework",
  sharedPackages = List("scalaprops")
):
  override def isBackendSupported(backend: Backend): Boolean = backend match
    case ScalaNativeBackend => false
    case ScalaJSBackend     => false
    case _                  => true
 