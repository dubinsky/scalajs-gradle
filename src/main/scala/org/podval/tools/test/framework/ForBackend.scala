package org.podval.tools.test.framework

import org.podval.tools.build.DependencyMaker

final class ForBackend(
  val maker: DependencyMaker,
  val underlying: Option[DependencyMaker] = None
)
