package org.podval.tools.nonjvm

import org.podval.tools.build.{ScalaBackend, ScalaVersion, SourceMapper, TestEnvironment}
import org.podval.tools.test.framework.FrameworkDescriptor
import sbt.testing.Framework

class NonJvmTestEnvironment[B <: ScalaBackend, A](
  backend: B,
  testAdapter: A,
  loadFrameworksFromTestAdapter: (A, List[List[String]]) => List[Option[Framework]],
  closeTestAdapter: A => Unit,
  sourceMapper: Option[SourceMapper]
) extends TestEnvironment[B](
  backend = backend,
  sourceMapper = sourceMapper
):
  override def close(): Unit = closeTestAdapter(testAdapter)

  override protected def loadFrameworks: List[Framework] = loadFrameworksFromTestAdapter(
    testAdapter,
    frameworksToLoad((descriptor: FrameworkDescriptor) => List(descriptor.className))
  ).flatten
