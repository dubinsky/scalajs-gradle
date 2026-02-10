package org.podval.tools.scalanative

import org.podval.tools.nonjvm.Named

enum Mode(name: String) extends Named(name) derives CanEqual:
  case Debug       extends Mode("debug")
  case ReleaseFast extends Mode("release-fast")
  case ReleaseSize extends Mode("release-size")
  case ReleaseFull extends Mode("release-full")

object Mode extends Named.Companion[Mode]("Mode", key = Some("SCALANATIVE_MODE")):
  override def default: Mode = Debug
  override def all: Seq[Mode] = values.toSeq
