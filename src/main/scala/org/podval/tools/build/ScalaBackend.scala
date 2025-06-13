package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.podval.tools.backend.jvm.JvmBackend
import org.podval.tools.backend.scalajs.ScalaJSBackend
import org.podval.tools.backend.scalanative.ScalaNativeBackend
import org.podval.tools.build.{BackendDependencyRequirements, ScalaVersion}

object ScalaBackend:
  val sharedSourceRoot: String = "shared"
  def all: Set[ScalaBackend] = Set(JvmBackend, ScalaJSBackend, ScalaNativeBackend)
  def names: String = all.map(backend => s"${backend.name} (${backend.sourceRoot})").mkString(", ")
  def sourceRoots: String = all.map(_.sourceRoot).mkString(", ")

trait ScalaBackend derives CanEqual:
  final def is(name: String): Boolean =
    name.toLowerCase == this.name      .toLowerCase ||
    name.toLowerCase == this.sourceRoot.toLowerCase

  final def artifactSuffixString: String = artifactSuffixOpt.map(suffix => s"_$suffix").getOrElse("")

  final protected def describe(what: String): String = s"$name $what."

  def name: String
  def sourceRoot: String
  def testsCanNotBeForked: Boolean
  def artifactSuffixOpt: Option[String]
  def archiveAppendixOpt: Option[String]
  
  def scalaCompileParameters(scalaVersion: ScalaVersion): Seq[String]

  def dependencyRequirements(
    implementationConfiguration: Configuration,
    testImplementationConfiguration: Configuration,
    scalaVersion: ScalaVersion
  ): BackendDependencyRequirements
