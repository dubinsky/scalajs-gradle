package org.podval.tools.backend.scalanative

import java.nio.file.Path
import scala.scalanative.build.Config

final class ScalaNativeLinkConfig(config: Config):
  def artifactName: String = config.artifactName
  def artifactPath: Path   = config.artifactPath
  
  def link(
    logSource: String,
    abort: String => Nothing
  ): Path = ScalaNativeBuild.link(
    config,
    logSource,
    abort
  )
