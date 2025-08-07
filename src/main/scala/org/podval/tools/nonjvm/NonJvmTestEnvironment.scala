package org.podval.tools.nonjvm

import org.podval.tools.build.ScalaBackend
import org.podval.tools.test.framework.Framework
import org.podval.tools.test.task.{SourceMapper, TestEnvironment}
import sbt.testing.Framework => FrameworkSBT

final class NonJvmTestEnvironment[B <: ScalaBackend, A](
  backend: B,
  testAdapter: A,
  loadFrameworksFromTestAdapter: (A, List[List[String]]) => List[Option[FrameworkSBT]],
  closeTestAdapter: A => Unit,
  sourceMapper: Option[SourceMapper]
) extends TestEnvironment[B](
  backend = backend,
  sourceMapper = sourceMapper
):
  override def close(): Unit = closeTestAdapter(testAdapter)

  override protected def loadFrameworks: List[Framework.Loaded] = loadFrameworksFromTestAdapter(
    testAdapter,
    frameworks.map((framework: Framework) => List(framework.className))
  )
    .flatten
    .map(Framework.forSBTFramework)
