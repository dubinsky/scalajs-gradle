package org.podval.tools.test.framework

import org.podval.tools.build.{Backend, ScalaDependency}
import org.podval.tools.nonjvm.NonJvmBackend

abstract class NonJvmJUnit4Framework(
  backend: NonJvmBackend,
  transform: ScalaDependency => ScalaDependency,
  artifact: String,
  nameSbt: String,
  className: String,
  sharedPackages: List[String]
) extends Framework(
  name = s"JUnit4 for ${backend.name}",
  nameSbt = nameSbt,
  className = className,
  sharedPackages = sharedPackages,
  tagOptions = None,
  usesTestSelectorAsNested = JUnit4Jvm.usesTestSelectorAsNested,
  additionalOptions = Array.empty
):
  final override def isBackendSupported(backend: Backend): Boolean = backend == this.backend

  final override def dependency: ScalaDependency = transform(
    backend.scalaDependency(
      what = "JUnit4", 
      artifact = artifact
    )
  )
