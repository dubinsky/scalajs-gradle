package org.podval.tools.backendplugin

import org.podval.tools.backend.ScalaBackend
import org.podval.tools.build.CreateExtension

trait BackendDelegate[T <: BackendTask]:
  def backend: ScalaBackend
  def pluginDependenciesConfigurationNameOpt: Option[String]
  def createExtension: Option[CreateExtension[?]]

  def linkTaskClassOpt    : Option[Class[? <: T & BackendTask.Link.Main]]
  def testLinkTaskClassOpt: Option[Class[? <: T & BackendTask.Link.Test]]
  def runTaskClassOpt     : Option[Class[? <: T & BackendTask.Run .Main]]
  def testTaskClass       :        Class[? <: T & BackendTask.Run .Test]
