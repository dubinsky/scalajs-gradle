package org.podval.tools.backendplugin.jvm

import org.podval.tools.backend.jvm.JvmBackend
import org.podval.tools.backendplugin.{BackendDelegate, BackendTask}
import org.podval.tools.build.CreateExtension

object JvmDelegate extends BackendDelegate[JvmTask]:
  override def backend: JvmBackend.type = JvmBackend
  override def pluginDependenciesConfigurationNameOpt: Option[String] = None
  override def createExtension: Option[CreateExtension[?]] = None

  override def testTaskClass: Class[JvmTestTask] = classOf[JvmTestTask]

  override def linkTaskClassOpt    : Option[Class[? <: JvmTask & BackendTask.Link.Main]] = None
  override def testLinkTaskClassOpt: Option[Class[? <: JvmTask & BackendTask.Link.Test]] = None
  override def runTaskClassOpt     : Option[Class[? <: JvmTask & BackendTask.Run .Main]] = None
