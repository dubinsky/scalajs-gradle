package org.podval.tools.scalaplugin.nonjvm

import org.podval.tools.build.nonjvm.NonJvmBackend
import org.podval.tools.scalaplugin.BackendDelegate

trait NonJvmDelegate[T <: NonJvmTask[?]] extends BackendDelegate[T]:
  override def backend: NonJvmBackend

  final override def linkTaskClassOpt    : Option[Class[? <: T & NonJvmLinkTask.Main[?]]] = Some(linkTaskClass    )
  final override def testLinkTaskClassOpt: Option[Class[? <: T & NonJvmLinkTask.Test[?]]] = Some(testLinkTaskClass)
  final override def runTaskClassOpt     : Option[Class[? <: T & NonJvmRunTask .Main[?]]] = Some(runTaskClass     )

  def linkTaskClass    : Class[? <: T & NonJvmLinkTask.Main[?]]
  def testLinkTaskClass: Class[? <: T & NonJvmLinkTask.Test[?]]
  def runTaskClass     : Class[? <: T & NonJvmRunTask .Main[?]]

  final override def pluginDependenciesConfigurationNameOpt: Option[String] = Some(pluginDependenciesConfigurationName)
  def pluginDependenciesConfigurationName: String
