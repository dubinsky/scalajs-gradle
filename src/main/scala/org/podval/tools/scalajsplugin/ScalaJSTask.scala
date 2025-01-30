package org.podval.tools.scalajsplugin

import org.podval.tools.node.{Node, TaskWithNode}
import org.podval.tools.scalajs.ScalaJS
import scala.jdk.CollectionConverters.SetHasAsScala

trait ScalaJSTask extends TaskWithNode:
  setDescription(s"$flavour ScalaJS")

  protected def flavour: String

  protected def linkTask: ScalaJSLinkTask

  protected final def scalaJS: ScalaJS =
    val node: Node = linkTask.node
    ScalaJS(
      nodePath = node.installation.node.getAbsolutePath,
      nodeEnvironment = node.nodeEnv.toMap,
      jsDirectory = linkTask.getJSDirectory,
      reportBinFile = linkTask.getReportBinFile,
      reportTextFile = linkTask.getReportTextFile,
      optimization = linkTask.optimization,
      moduleKind = linkTask.moduleKind,
      moduleSplitStyle = linkTask.moduleSplitStyle,
      moduleInitializers = linkTask.moduleInitializers,
      prettyPrint = linkTask.prettyPrint,
      runtimeClassPath = linkTask.getRuntimeClassPath.getFiles.asScala.toSeq,
      logger = getLogger,
      logSource = s"ScalaJS $getName"
    )
