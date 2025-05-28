package org.podval.tools.scalaplugin.scalanative

import org.podval.tools.build.scalanative.ScalaNativeBackend
import org.podval.tools.build.CreateExtension
import org.podval.tools.scalaplugin.nonjvm.NonJvmDelegate

object ScalaNativeDelegate extends NonJvmDelegate[ScalaNativeTask]:
  override def backend: ScalaNativeBackend.type = ScalaNativeBackend
  override def pluginDependenciesConfigurationName: String = "scalanative"
  override def createExtension: Option[CreateExtension[?]] = None

  override def linkTaskClass    : Class[ScalaNativeLinkMainTask] = classOf[ScalaNativeLinkMainTask]
  override def testLinkTaskClass: Class[ScalaNativeLinkTestTask] = classOf[ScalaNativeLinkTestTask]
  override def runTaskClass     : Class[ScalaNativeRunMainTask ] = classOf[ScalaNativeRunMainTask ]
  override def testTaskClass    : Class[ScalaNativeTestTask    ] = classOf[ScalaNativeTestTask    ]
