package org.podval.tools.scalajs

import org.gradle.api.GradleException
import java.io.File
import scala.jdk.CollectionConverters.*

trait AfterLinkTask extends ScalaJSTask:
  protected def linkTaskClass: Class[? <: LinkTask]

  final def linkTask: LinkTask = getDependsOn
    .asScala
    .find(candidate => linkTaskClass.isAssignableFrom(candidate.getClass))
    .map(_.asInstanceOf[LinkTask])
    .getOrElse(throw new GradleException(s"Task $getName must depend on a task of type ${linkTaskClass.getName}!"))

  protected def createAfterLink: AfterLink =
    expandClassPath()

    val nodePath: String = File(getProject.getProjectDir, "node_modules").getAbsolutePath

    AfterLink(
      moduleKindProperty = linkTask.getModuleKind,
      reportBinFile = linkTask.getReportBinFile,
      jsDirectory = linkTask.getJSDirectory,
      taskName = getName,
      logger = getLogger, // TODO DO NOT PASS THE LOGGER IN?
      nodePath = nodePath
    )
