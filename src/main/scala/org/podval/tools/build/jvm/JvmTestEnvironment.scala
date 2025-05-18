package org.podval.tools.build.jvm

import org.podval.tools.test.environment.{SourceMapper, TestEnvironment}
import org.podval.tools.test.framework.{FrameworkDescriptor, FrameworkProvider}
import sbt.testing.Framework

final class JvmTestEnvironment extends TestEnvironment:
  override def backend: JvmBackend.type = JvmBackend

  override def close(): Unit = ()

  override def sourceMapper: Option[SourceMapper] = None

  protected def expandClassPath: Boolean = true
  
  override protected def loadFrameworks: List[Framework] = FrameworkDescriptor
    .forBackend(backend)
    .flatMap(FrameworkProvider(_).frameworkOpt)
