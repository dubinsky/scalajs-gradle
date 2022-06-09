package org.podval.tools.scalajs

enum Stage(
  val description: String,
  val outputDirectory: String
) derives CanEqual:

  case FastOpt extends Stage(
    description = " - fast",
    outputDirectory = "fastopt"
  )

  case FullOpt extends Stage(
    description = " - full optimization",
    outputDirectory = "opt"
  )

object Stage:
  val all: List[Stage] = List(Stage.FastOpt, Stage.FullOpt)
