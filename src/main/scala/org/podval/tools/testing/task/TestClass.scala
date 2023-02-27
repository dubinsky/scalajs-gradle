package org.podval.tools.testing.task

import sbt.testing.{ Fingerprint, Framework}

class TestClass(
  val className: String,
  val sourceFilePath: String,
  val framework: Framework,
  val fingerprint: Fingerprint
)
