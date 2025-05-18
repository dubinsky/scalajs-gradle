package org.podval.tools.scalajsplugin.scalajs

import org.podval.tools.build.scalajs.ScalaJSBackend
import org.podval.tools.build.CreateExtension
import org.podval.tools.node.NodeExtension
import org.podval.tools.scalajsplugin.nonjvm.NonJvmDelegate
import scala.jdk.CollectionConverters.SeqHasAsJava

object ScalaJSDelegate extends NonJvmDelegate[ScalaJSTask]:
  override def backend: ScalaJSBackend.type = ScalaJSBackend

  override def taskClass        : Class[ScalaJSTask        ] = classOf[ScalaJSTask        ]
  override def linkTaskClass    : Class[ScalaJSLinkMainTask] = classOf[ScalaJSLinkMainTask]
  override def testLinkTaskClass: Class[ScalaJSLinkTestTask] = classOf[ScalaJSLinkTestTask]
  override def runTaskClass     : Class[ScalaJSRunMainTask ] = classOf[ScalaJSRunMainTask ]
  override def testTaskClass    : Class[ScalaJSTestTask    ] = classOf[ScalaJSTestTask    ]

  override def pluginDependenciesConfigurationName: String = "scalajs"

  override def createExtension: Some[CreateExtension[NodeExtension]] = Some(
    NodeExtension.create((nodeExtension: NodeExtension) =>
      nodeExtension.getModules.convention(List("jsdom").asJava)
    )
  )
