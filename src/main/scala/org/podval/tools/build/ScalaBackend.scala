package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.podval.tools.build.jvm.JvmBackend
import org.podval.tools.build.scalajs.ScalaJSBackend
import org.podval.tools.build.scalanative.ScalaNativeBackend

object ScalaBackend:
  val sharedSourceRoot: String = "shared"
  def all: Set[ScalaBackend] = Set(JvmBackend, ScalaJSBackend, ScalaNativeBackend)

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
  
  def scalaCompileParameters(isScala3: Boolean): Seq[String]

  def dependencyRequirements(
    implementationConfiguration: Configuration,
    testImplementationConfiguration: Configuration,
    projectScalaPlatform: ScalaPlatform
  ): BackendDependencyRequirements
