package org.podval.tools.nonjvm

import org.podval.tools.build.{SourceMapper, TestEnvironment, TestFramework}
import sbt.testing.Framework as FrameworkSBT

final class NonJvmTestEnvironment[B <: NonJvmBackend, A](
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

  override protected def loadFrameworks: List[TestFramework.Loaded] = loadFrameworksFromTestAdapter(
    testAdapter,
    frameworks.map((framework: TestFramework) => List(framework.className))
  )
    .flatten
    .map((frameworkSBT: FrameworkSBT) => TestFramework.forNameSbt(frameworkSBT.name).loaded(frameworkSBT))
