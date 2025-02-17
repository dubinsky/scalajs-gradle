package org.podval.tools.scalajsplugin

import org.gradle.api.GradleException
import org.podval.tools.node.TaskWithNode
import org.podval.tools.scalajs.ScalaJSCommon
import scala.jdk.CollectionConverters.SetHasAsScala

trait ScalaJSTask extends TaskWithNode:
  setDescription(s"$flavour ScalaJS")

  protected def flavour: String

  protected def linkTask: ScalaJSLinkTask

  protected final def scalaJSCommon: ScalaJSCommon = ScalaJSCommon(
    jsDirectory = linkTask.getJSDirectory,
    reportBinFile = linkTask.getReportBinFile,
    moduleKind = linkTask.moduleKind,
    logger = getLogger,
    logSource = s"ScalaJS $getName",
    abort = (message: String) => GradleException(message)
  )
