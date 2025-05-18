package org.podval.tools.build.scalanative

import org.podval.tools.build.nonjvm.{NonJvmTestAdapter, NonJvmTestEnvironment}
import org.podval.tools.test.environment.SourceMapper

final class ScalaNativeTestEnvironment(
  sourceMapper: Option[SourceMapper],
  testAdapter: NonJvmTestAdapter
) extends NonJvmTestEnvironment(
  testAdapter,
  sourceMapper
):
  override def backend: ScalaNativeBackend.type = ScalaNativeBackend
