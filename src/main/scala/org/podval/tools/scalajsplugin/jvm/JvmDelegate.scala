package org.podval.tools.scalajsplugin.jvm

import org.podval.tools.build.CreateExtension
import org.podval.tools.build.jvm.JvmBackend
import org.podval.tools.scalajsplugin.{BackendDelegate, BackendTask}

object JvmDelegate extends BackendDelegate[JvmTask]:
  override def backend: JvmBackend.type = JvmBackend

  override def taskClass: Class[JvmTask] = classOf[JvmTask]

  override def linkTaskClassOpt    : Option[Class[? <: JvmTask & BackendTask.Link.Main]] = None
  override def testLinkTaskClassOpt: Option[Class[? <: JvmTask & BackendTask.Link.Test]] = None

  override def runTaskClass : Class[JvmRunTask ] = classOf[JvmRunTask ]
  override def testTaskClass: Class[JvmTestTask] = classOf[JvmTestTask]

  override def pluginDependenciesConfigurationNameOpt: Option[String] = None
  override def createExtension: Option[CreateExtension[?]] = None
