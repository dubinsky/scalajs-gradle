package org.podval.tools.scalajsplugin

import org.podval.tools.build.{CreateExtension, ScalaBackend}

trait BackendDelegate[T <: BackendTask]:
  def backend: ScalaBackend

  // TODO capture class tag or something
  def taskClass: Class[? <: T]
  
  def linkTaskClassOpt    : Option[Class[? <: T & BackendTask.Link.Main]]
  def testLinkTaskClassOpt: Option[Class[? <: T & BackendTask.Link.Test]]
  def runTaskClassOpt     : Option[Class[? <: T & BackendTask.Run .Main]]
  def testTaskClass       :        Class[? <: T & BackendTask.Run .Test]

  def pluginDependenciesConfigurationNameOpt: Option[String]
  
  def createExtension: Option[CreateExtension[?]]
