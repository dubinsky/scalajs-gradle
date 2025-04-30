package org.podval.tools.scalanative

import org.podval.tools.util.Named

enum GC(name: String) extends Named(name) derives CanEqual:
  case None   extends GC("none")
  case Boehm  extends GC("boehm")
  case Immix  extends GC("immix")
  case Commix extends GC("commix")

object GC extends Named.Companion[GC]("GC", key = Some("SCALANATIVE_GC")):
  override def default: GC = Immix
  override def all: Seq[GC] = values.toSeq
