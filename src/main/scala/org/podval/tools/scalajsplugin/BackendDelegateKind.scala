package org.podval.tools.scalajsplugin

import org.gradle.api.Project
import org.podval.tools.build.ScalaBackendKind
import org.podval.tools.scalajsplugin.jvm.JvmDelegate
import org.podval.tools.scalajsplugin.scalajs.ScalaJSDelegate
import org.podval.tools.scalajsplugin.scalanative.ScalaNativeDelegate

sealed class BackendDelegateKind(
  val name: String,
  val sourceRoot: String,
  val mk: (project: Project, isModeMixed: Boolean) => BackendDelegate,
  val backendKind: ScalaBackendKind,
  val gradleNamesSuffix: String,
  val isCreateForMixedMode: Boolean
)

object BackendDelegateKind:
  val sharedSourceRoot: String = "shared"

  def all: Set[BackendDelegateKind] = Set(JVM, JS, Native)

  object JVM extends BackendDelegateKind(
    name = "JVM",
    sourceRoot = "jvm",
    mk = JvmDelegate.apply,
    backendKind = ScalaBackendKind.JVM,
    gradleNamesSuffix = "",
    isCreateForMixedMode = false
  )
  
  object JS extends BackendDelegateKind(
    name = "JS",
    sourceRoot = "js", 
    mk = ScalaJSDelegate.apply,
    backendKind = ScalaBackendKind.JS,
    gradleNamesSuffix = "JS",
    isCreateForMixedMode = true
  )
  
  object Native extends BackendDelegateKind(
    name = "Native",
    sourceRoot = "native", 
    mk = ScalaNativeDelegate.apply,
    backendKind = ScalaBackendKind.Native,
    gradleNamesSuffix = "Native",
    isCreateForMixedMode = true
  )
