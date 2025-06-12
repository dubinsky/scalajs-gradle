package org.podval.tools.backendplugin.scalanative

import org.podval.tools.backend.scalanative.ScalaNativeBackend
import org.podval.tools.backendplugin.nonjvm.NonJvmDelegate
import org.podval.tools.build.CreateExtension

object ScalaNativeDelegate extends NonJvmDelegate[ScalaNativeTask]:
  override def backend: ScalaNativeBackend.type = ScalaNativeBackend
  override def pluginDependenciesConfigurationName: String = "scalanative"
  override def createExtension: Option[CreateExtension[?]] = None

  override def linkTaskClass    : Class[ScalaNativeLinkMainTask] = classOf[ScalaNativeLinkMainTask]
  override def testLinkTaskClass: Class[ScalaNativeLinkTestTask] = classOf[ScalaNativeLinkTestTask]
  override def runTaskClass     : Class[ScalaNativeRunMainTask ] = classOf[ScalaNativeRunMainTask ]
  override def testTaskClass    : Class[ScalaNativeTestTask    ] = classOf[ScalaNativeTestTask    ]
