package org.podval.tools.scalajsplugin

import org.gradle.api.{GradleException, Project}
import org.gradle.api.provider.Property
import org.podval.tools.build.{ScalaBinaryVersion, ScalaVersion}

final class ScalaVersionData(scalaVersion: ScalaVersion):
  def getVersion: String = scalaVersion.version.toString
  def isScala3: Boolean = scalaVersion.isScala3
  def getMajor: Int = scalaVersion.binaryVersion.versionMajor
  def getBinary: String = scalaVersion.binaryVersion.versionSuffix.toString
  def getBinary2: String =
    if !isScala3
    then getBinary
    else ScalaBinaryVersion.Scala213.versionSuffix.toString

object ScalaVersionData:
  def get(project: Project): ScalaVersionData = ScalaVersionData(
    Option(ScalaJSPlugin.getScalaExtensionScalaVersionProperty(project).getOrNull)
      .map(ScalaVersion(_))
      .getOrElse(throw GradleException(ScalaJSPlugin.helpMessage(project,
        s"""retrieving `ScalaVersionData`
           |is not supported when Scala version is inferred from the Scala library dependency;
           |set Scala version on the Scala plugin's extension instead: `scala.scalaVersion=...`""".stripMargin
      )))
  )
