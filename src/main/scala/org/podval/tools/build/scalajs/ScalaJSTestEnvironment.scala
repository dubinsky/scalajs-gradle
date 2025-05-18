package org.podval.tools.build.scalajs

import org.podval.tools.build.nonjvm.{NonJvmTestAdapter, NonJvmTestEnvironment}
import org.podval.tools.test.environment.SourceMapper

final class ScalaJSTestEnvironment(
  sourceMapper: Option[SourceMapper],
  testAdapter: NonJvmTestAdapter,
) extends NonJvmTestEnvironment(
  testAdapter,
  sourceMapper
):
  override def backend: ScalaJSBackend.type = ScalaJSBackend
