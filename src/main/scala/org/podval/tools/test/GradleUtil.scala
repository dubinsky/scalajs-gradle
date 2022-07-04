package org.podval.tools.test

import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.Project
import org.opentorah.build.{DependencyRequirement, ScalaLibrary}
import org.opentorah.build.Gradle.*

object GradleUtil:
  // TODO switch to ScalaLibrary.Scala3SJS.forVersions() after the next OpenTorah release
  def scalaJSlibraryRequirement(
    scalaLibrary: ScalaLibrary
  ): Option[DependencyRequirement] = if !scalaLibrary.isScala3 then None else Some(
    DependencyRequirement(
      dependency = ScalaLibrary.Scala3SJS,
      version = scalaLibrary.scala3.get.version,
      scalaLibrary = scalaLibrary,
      reason = "because it is needed for linking of the ScalaJS code"
    )
  )

  extension(project: Project)
    // TODO use the one from OpenTorah after its next release
    def getScalaCompileForSourceSet(sourceSetName: String): ScalaCompile =
      project.getScalaCompile(project.getSourceSet(sourceSetName))
