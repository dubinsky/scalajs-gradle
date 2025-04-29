package org.podval.tools.scalajsplugin.scalanative

import org.podval.tools.test.environment.{SourceMapper, TestEnvironment}
import sbt.testing.Framework

final class ScalaNativeTestEnvironment extends TestEnvironment:
  override def close(): Unit = ()

  protected def expandClassPath: Boolean = false
  
  override protected def loadFrameworks: List[Framework] = ???

  override def sourceMapper: Option[SourceMapper] = None
  