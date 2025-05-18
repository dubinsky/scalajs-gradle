package org.podval.tools.build.scalanative

import java.nio.file.Path
import scala.scalanative.build.Config

final class ScalaNativeLinkConfig(config: Config):
  def artifactName: String = config.artifactName
  def artifactPath: Path   = config.artifactPath
  
  def link(logSource: String): Path = ScalaNativeBuild.link(
    config,
    logSource
  )
