package org.podval.tools.test

import org.gradle.internal.id.CompositeIdGenerator.CompositeId

object RootTestSuiteIdPlaceholder:
  // Note: Since I can not use the real `rootTestSuiteId` that `DefaultTestExecuter` supplies to the `TestMainAction` -
  // because it is a `String` - and I am not keen on second-guessing what it is anyway,
  // I use a `idPlaceholder` in `WorkerTestClassProcessor`
  // and change it to the real one in `FixUpRootTestOutputTestResultProcessor`.
  val value: CompositeId = CompositeId(0L, 0L)
  