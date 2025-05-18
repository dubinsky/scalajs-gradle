package org.podval.tools.scalajsplugin.nonjvm

import org.podval.tools.build.nonjvm.NonJvmBackend
import org.podval.tools.scalajsplugin.BackendDelegate

trait NonJvmDelegate[T <: NonJvmTask[?]] extends BackendDelegate[T]:
  override def backend: NonJvmBackend

  final override def linkTaskClassOpt    : Option[Class[? <: T & NonJvmLinkTask.Main[?]]] = Some(linkTaskClass    )
  final override def testLinkTaskClassOpt: Option[Class[? <: T & NonJvmLinkTask.Test[?]]] = Some(testLinkTaskClass)

  def linkTaskClass    : Class[? <: T & NonJvmLinkTask.Main[?]]
  def testLinkTaskClass: Class[? <: T & NonJvmLinkTask.Test[?]]

  final override def pluginDependenciesConfigurationNameOpt: Option[String] = Some(pluginDependenciesConfigurationName)
  def pluginDependenciesConfigurationName: String
