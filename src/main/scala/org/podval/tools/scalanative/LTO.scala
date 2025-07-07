package org.podval.tools.scalanative

import org.podval.tools.build.Named

enum LTO(name: String) extends Named(name) derives CanEqual:
  case None extends LTO("none")
  case Thin extends LTO("thin")
  case Full extends LTO("full")

object LTO extends Named.Companion[LTO]("LTO", key = Some("SCALANATIVE_LTO")):
  override def default: LTO = None
  override def all: Seq[LTO] = values.toSeq
