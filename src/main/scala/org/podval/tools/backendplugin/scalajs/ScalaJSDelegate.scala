package org.podval.tools.backendplugin.scalajs

import org.podval.tools.backend.scalajs.ScalaJSBackend
import org.podval.tools.backendplugin.nonjvm.NonJvmDelegate
import org.podval.tools.build.CreateExtension
import org.podval.tools.node.NodeExtension

object ScalaJSDelegate extends NonJvmDelegate[ScalaJSTask]:
  override def backend: ScalaJSBackend.type = ScalaJSBackend
  override def pluginDependenciesConfigurationName: String = "scalajs"
  override def createExtension: Some[CreateExtension[NodeExtension]] = Some(NodeExtension.create)

  override def linkTaskClass    : Class[ScalaJSLinkMainTask] = classOf[ScalaJSLinkMainTask]
  override def testLinkTaskClass: Class[ScalaJSLinkTestTask] = classOf[ScalaJSLinkTestTask]
  override def runTaskClass     : Class[ScalaJSRunMainTask ] = classOf[ScalaJSRunMainTask ]
  override def testTaskClass    : Class[ScalaJSTestTask    ] = classOf[ScalaJSTestTask    ]
