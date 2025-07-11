package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackend, ScalaDependencyMaker}

trait ScalaFrameworkDescriptor extends FrameworkDescriptor with ScalaDependencyMaker:
  override def maker(backend: ScalaBackend): Option[ScalaDependencyMaker] = Some(
    new ScalaDependencyMaker.Delegating(ScalaFrameworkDescriptor.this):
      override def scalaBackend: ScalaBackend = backend
  )
