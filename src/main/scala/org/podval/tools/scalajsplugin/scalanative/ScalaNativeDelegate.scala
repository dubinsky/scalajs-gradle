package org.podval.tools.scalajsplugin.scalanative

import org.podval.tools.build.scalanative.ScalaNativeBackend
import org.podval.tools.build.CreateExtension
import org.podval.tools.scalajsplugin.nonjvm.NonJvmDelegate

object ScalaNativeDelegate extends NonJvmDelegate[ScalaNativeTask]:
  override def backend: ScalaNativeBackend.type = ScalaNativeBackend

  override def taskClass        : Class[ScalaNativeTask        ] = classOf[ScalaNativeTask        ]
  override def linkTaskClass    : Class[ScalaNativeLinkMainTask] = classOf[ScalaNativeLinkMainTask]
  override def testLinkTaskClass: Class[ScalaNativeLinkTestTask] = classOf[ScalaNativeLinkTestTask]
  override def runTaskClass     : Class[ScalaNativeRunMainTask ] = classOf[ScalaNativeRunMainTask ]
  override def testTaskClass    : Class[ScalaNativeTestTask    ] = classOf[ScalaNativeTestTask    ]

  override def pluginDependenciesConfigurationName: String = "scalanative"
  override def createExtension: Option[CreateExtension[?]] = None
