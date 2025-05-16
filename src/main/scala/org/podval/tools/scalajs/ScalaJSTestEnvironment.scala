package org.podval.tools.scalajs

import org.podval.tools.build.ScalaBackendKind
import org.podval.tools.nonjvm.{NonJvmTestAdapter, NonJvmTestEnvironment}
import org.podval.tools.test.environment.SourceMapper

final class ScalaJSTestEnvironment(
  sourceMapper: Option[SourceMapper],
  testAdapter: NonJvmTestAdapter,
) extends NonJvmTestEnvironment(
  testAdapter,
  sourceMapper
):
  override def backend: ScalaBackendKind = ScalaBackendKind.JS
