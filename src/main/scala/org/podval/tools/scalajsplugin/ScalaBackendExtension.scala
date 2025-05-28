package org.podval.tools.scalajsplugin

import org.gradle.api.{GradleException, Project}
import org.gradle.api.provider.Property
import org.podval.tools.build.{ScalaBackend, ScalaBinaryVersion, ScalaVersion}
import org.podval.tools.build.jvm.JvmBackend
import org.podval.tools.build.scalajs.ScalaJSBackend
import org.podval.tools.build.scalanative.ScalaNativeBackend
import javax.inject.Inject

abstract class ScalaBackendExtension  @Inject(project: Project):
//  def getName: Property[String]
//  def getVersion: Property[String]
  private lazy val backend: ScalaBackend = ScalaBackendExtension.getBackend(project)
  // TODO would be nice to see the version inferred...
  def getSuffix: String = backend.artifactSuffixString
  def isJvm: Boolean = backend == JvmBackend
  def isJs: Boolean = backend == ScalaJSBackend
  def isNative: Boolean = backend == ScalaNativeBackend
  
  private lazy val scalaVersion = ScalaBackendExtension.getScalaVersion(project)
  def getVersion: String = scalaVersion.version.toString
  def isScala3: Boolean = scalaVersion.isScala3
  def getMajor: Int = scalaVersion.binaryVersion.versionMajor
  def getBinary: String = scalaVersion.binaryVersion.versionSuffix.toString
  def getBinary2: String =
    if !isScala3
    then getBinary
    else ScalaBinaryVersion.Scala213.versionSuffix.toString

object ScalaBackendExtension:
  private def getBackend(project: Project): ScalaBackend = 
    Option(project.findProperty(ScalaJSPlugin.scalaBackendProperty))
      .map(_.toString)
      .flatMap((name: String) => ScalaBackend.all.find(_.is(name)))
      .getOrElse(throw GradleException(ScalaJSPlugin.helpMessage(project,
        s"Scala backend data is not available when property `${ScalaJSPlugin.scalaBackendProperty}` is not set"
      )))
    
  private def getScalaVersion(project: Project): ScalaVersion =
    Option(ScalaJSPlugin.getScalaExtensionScalaVersionProperty(project).getOrNull)
      .map(ScalaVersion(_))
      .getOrElse(throw GradleException(ScalaJSPlugin.helpMessage(project,
        s"""Scala version data is not supported when Scala version is inferred from the Scala library dependency;
           |set Scala version on the Scala plugin's extension instead: `scala.scalaVersion=...`""".stripMargin
      )))
