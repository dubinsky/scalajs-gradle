package org.podval.tools.backend.scalanative

import org.podval.tools.backend.nonjvm.{NonJvmTestAdapter, NonJvmTestEnvironment}
import org.podval.tools.test.environment.SourceMapper

final class ScalaNativeTestEnvironment(
  sourceMapper: Option[SourceMapper],
  testAdapter: NonJvmTestAdapter
) extends NonJvmTestEnvironment(
  testAdapter,
  sourceMapper
):
  override def backend: ScalaNativeBackend.type = ScalaNativeBackend
