package org.podval.tools.backendplugin.scalajs

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.podval.tools.backend.scalajs.{BrowserName, JSEnvKind}
import org.podval.tools.node.Node
import org.podval.tools.backendplugin.nonjvm.NonJvmRunTask

trait ScalaJSRunTask extends NonJvmRunTask[ScalaJSLinkTask] with ScalaJSTask:
  @Input def getJsEnv: Property[String]
  JSEnvKind.convention(getJsEnv)
  def jsEnvKind: JSEnvKind = JSEnvKind(getJsEnv)
  
  @Input def getBrowserName: Property[String]
  BrowserName.convention(getBrowserName)
  def browserName: BrowserName = BrowserName(getBrowserName)
