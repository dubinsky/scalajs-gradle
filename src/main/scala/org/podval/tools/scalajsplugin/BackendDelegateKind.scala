package org.podval.tools.scalajsplugin

import org.gradle.api.Project
import org.podval.tools.build.ScalaBackendKind

class BackendDelegateKind(
  val sourceRoot: String,
  val backendKind: ScalaBackendKind,
  val mk: (project: Project, isModeMixed: Boolean) => BackendDelegate,
)

object BackendDelegateKind:
  val sharedSourceRoot: String = "shared"

  def all: Set[BackendDelegateKind] = Set(
    org.podval.tools.scalajsplugin.jvm.JvmDelegate,
    org.podval.tools.scalajsplugin.scalajs.ScalaJSDelegate,
    org.podval.tools.scalajsplugin.scalanative.ScalaNativeDelegate
  )
