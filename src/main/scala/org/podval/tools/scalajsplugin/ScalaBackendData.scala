package org.podval.tools.scalajsplugin

import org.gradle.api.{GradleException, Project}
import org.podval.tools.build.ScalaBackend
import org.podval.tools.build.jvm.JvmBackend
import org.podval.tools.build.scalajs.ScalaJSBackend
import org.podval.tools.build.scalanative.ScalaNativeBackend

final class ScalaBackendData(scalaBackend: ScalaBackend):
  // TODO would be nice to see the version inferred...
  def getSuffix: String = scalaBackend.artifactSuffixString
  def isJvm: Boolean = scalaBackend == JvmBackend
  def isJs: Boolean = scalaBackend == ScalaJSBackend
  def isNative: Boolean = scalaBackend == ScalaNativeBackend

object ScalaBackendData:
  def get(project: Project): ScalaBackendData = ScalaBackendData(
    Option(project.findProperty(ScalaJSPlugin.scalaBackendProperty))
      .map(_.toString)
      .flatMap((name: String) => ScalaBackend.all.find(_.is(name)))
      .getOrElse(throw GradleException(ScalaJSPlugin.helpMessage(project,
        s"""retrieving `ScalaBackendData`
           |is not supported when property `${ScalaJSPlugin.scalaBackendProperty}` is not set""".stripMargin
      )))
  )
