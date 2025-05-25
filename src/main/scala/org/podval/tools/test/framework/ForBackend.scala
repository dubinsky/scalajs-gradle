package org.podval.tools.test.framework

import org.podval.tools.build.Dependency

final class ForBackend(
  val maker: Dependency.Maker,
  val underlying: Option[Dependency.Maker] = None
)
