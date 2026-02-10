package org.podval.tools.nonjvm

import org.podval.tools.build.{Backend, ScalaDependency, TestFramework}
import org.podval.tools.jvm.JUnit4Jvm
import org.podval.tools.nonjvm.NonJvmBackend

abstract class NonJvmJUnit4TestFramework(
  backend: NonJvmBackend,
  transform: ScalaDependency => ScalaDependency,
  artifact: String,
  nameSbt: String,
  className: String,
  sharedPackages: List[String]
) extends TestFramework(
  name = s"JUnit4-${backend.name}",
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
