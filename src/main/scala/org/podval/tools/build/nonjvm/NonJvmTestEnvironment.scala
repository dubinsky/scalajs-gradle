package org.podval.tools.build.nonjvm

import org.podval.tools.test.environment.{SourceMapper, TestEnvironment}
import org.podval.tools.test.framework.FrameworkDescriptor
import sbt.testing.Framework

abstract class NonJvmTestEnvironment(
  testAdapter: NonJvmTestAdapter,
  override val sourceMapper: Option[SourceMapper]
) extends TestEnvironment:
  override def backend: NonJvmBackend

  final override protected def expandClassPath: Boolean = false

  final override def close(): Unit = testAdapter.close()

  final override protected def loadFrameworks: List[Framework] = testAdapter
    .loadFrameworks(FrameworkDescriptor
      .forBackend(backend)
      .map((descriptor: FrameworkDescriptor) => List(descriptor.className))
    )
    .flatten
