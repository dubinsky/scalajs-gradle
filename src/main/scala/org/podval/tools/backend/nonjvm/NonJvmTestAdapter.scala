package org.podval.tools.backend.nonjvm

import sbt.testing.Framework

trait NonJvmTestAdapter:
  def loadFrameworks(frameworkNames: List[List[String]]): List[Option[Framework]]

  def close(): Unit
