package org.podval.tools.test.framework

import org.podval.tools.build.Dependency

final class ForBackend(
  val isSupported: Boolean = true,
  val underlying: Option[Dependency.Maker[?]] = None
)

object ForBackend:
  val notSupported: ForBackend = ForBackend(isSupported = false)
