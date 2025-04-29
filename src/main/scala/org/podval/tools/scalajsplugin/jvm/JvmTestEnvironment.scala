package org.podval.tools.scalajsplugin.jvm

import org.podval.tools.build.ScalaBackendKind
import org.podval.tools.test.environment.{SourceMapper, TestEnvironment}
import org.podval.tools.test.framework.{FrameworkDescriptor, FrameworkProvider}
import sbt.testing.Framework

final class JvmTestEnvironment extends TestEnvironment:
  override def close(): Unit = ()

  override def sourceMapper: Option[SourceMapper] = None

  protected def expandClassPath: Boolean = true
  
  override protected def loadFrameworks: List[Framework] = FrameworkDescriptor
    .forBackend(ScalaBackendKind.JVM)
    .flatMap(FrameworkProvider(_).frameworkOpt)
