package org.podval.tools.test

import org.gradle.api.artifacts.Configuration
import org.opentorah.build.{Configurations, DependencyRequirement, Scala2Dependency, ScalaLibrary}
import scala.jdk.CollectionConverters.*

object Sbt:
  val group: String = "org.scala-sbt"

  object ZincPersist extends Scala2Dependency(group = group, nameBase = "zinc-persist")

  val configurationName: String = "sbt"
  val configurations: Configurations = Configurations.forName(configurationName)

  def getVersion(configuration: Configuration): String = ZincPersist
    .getFromClassPath(configuration.asScala)
    .get
    .version

  def requirements(
    pluginScalaLibrary: ScalaLibrary,
    sbtVersion: String
  ): Seq[DependencyRequirement] = Seq(
    DependencyRequirement(
      dependency = Sbt.ZincPersist,
      version = sbtVersion,
      scalaLibrary = pluginScalaLibrary,
      reason = "because it is needed for interfacing with sbt",
      configurations = Sbt.configurations
    )
  )
