package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.DefaultTestClassRunInfo
import sbt.testing.{Fingerprint, Framework, Selector}

// Note: This must extend DefaultTestClassRunInfo, not just TestClassRunInfo,
// because that is what it gets cast to in TestEventSerializer!
// Because of this, Test must be a trait.
// And to avoid "trait WithTaskDef may not call constructor of trait Test", it must be parameterless.
final class TestClass(
  parentId: Object,
  id: Object,
  val framework: Framework,
  className: String,
  fingerprint: Fingerprint,
  explicitlySpecified: Boolean,
  selectors: Array[Selector]
) extends DefaultTestClassRunInfo(
  className
) with FingerprintedTest(
  parentId = parentId,
  id = id,
  className,
  fingerprint,
  explicitlySpecified,
  selectors
):
  override def isComposite: Boolean = true
  override def getName: String = className
  override def toString: String = s"Suite $getClassName"
  override def getTestClassName: String = className
